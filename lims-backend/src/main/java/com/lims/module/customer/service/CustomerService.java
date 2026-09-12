package com.lims.module.customer.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.common.util.CodeGenerator;
import com.lims.module.customer.dto.CustomerQueryDTO;
import com.lims.module.customer.dto.CustomerSaveDTO;
import com.lims.module.customer.dto.CustomerVO;
import com.lims.module.customer.entity.Customer;
import com.lims.module.customer.entity.CustomerPoolLog;
import com.lims.module.customer.mapper.CustomerMapper;
import com.lims.module.customer.mapper.CustomerPoolLogMapper;
import com.lims.module.system.entity.SysUser;
import com.lims.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final CustomerPoolLogMapper poolLogMapper;
    private final SysUserMapper userMapper;
    private final CodeGenerator codeGenerator;

    @Transactional
    public Long save(CustomerSaveDTO dto) {
        Customer c = dto.getId() == null ? new Customer() : customerMapper.selectById(dto.getId());
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        c.setName(dto.getName());
        c.setShortName(dto.getShortName());
        c.setCustomerType(dto.getCustomerType());
        c.setIndustry(dto.getIndustry());
        c.setContactPerson(dto.getContactPerson());
        c.setContactPhone(dto.getContactPhone());
        c.setEmail(dto.getEmail());
        c.setProvince(dto.getProvince());
        c.setCity(dto.getCity());
        c.setDistrict(dto.getDistrict());
        c.setAddress(dto.getAddress());
        c.setBankName(dto.getBankName());
        c.setBankAccount(dto.getBankAccount());
        c.setTaxNo(dto.getTaxNo());
        c.setCustomerLevel(dto.getCustomerLevel());
        c.setCreditLimit(dto.getCreditLimit());
        c.setCreditPeriod(dto.getCreditPeriod());
        c.setSource(dto.getSource());
        c.setRemark(dto.getRemark());

        SecurityUtils.LoginUser user = SecurityUtils.current();
        if (dto.getId() == null) {
            c.setCode(codeGenerator.next("KH", false));
            c.setCreditScore(60);
            c.setStatus(1);
            c.setFollowCount(0);
            c.setPoolStatus(1);
            if (!Boolean.TRUE.equals(dto.getToPool())) {
                c.setOwnerUserId(user.getUserId());
                c.setPoolStatus(2);
                writePoolLog(c.getId(), "CLAIM", null, user.getUserId(), "新建即认领");
            }
            customerMapper.insert(c);
        } else {
            customerMapper.updateById(c);
        }
        return c.getId();
    }

    public Page<CustomerVO> page(CustomerQueryDTO q) {
        SecurityUtils.LoginUser user = SecurityUtils.current();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Customer> wrap =
                Wrappers.<Customer>lambdaQuery()
                .and(StringUtils.hasText(q.getKeyword()),
                        w -> w.like(Customer::getName, q.getKeyword())
                              .or().like(Customer::getCode, q.getKeyword())
                              .or().like(Customer::getContactPerson, q.getKeyword()))
                .eq(q.getCustomerLevel() != null, Customer::getCustomerLevel, q.getCustomerLevel())
                .eq(q.getCustomerType() != null, Customer::getCustomerType, q.getCustomerType())
                .eq(q.getPoolStatus() != null, Customer::getPoolStatus, q.getPoolStatus())
                .eq(q.getStatus() != null, Customer::getStatus, q.getStatus())
                .orderByDesc(Customer::getUpdateTime);

        switch (q.getScope() == null ? "all" : q.getScope()) {
            case "pool":
                wrap.isNull(Customer::getOwnerUserId);
                break;
            case "mine":
                wrap.eq(Customer::getOwnerUserId, user.getUserId());
                break;
            case "owner":
                wrap.eq(q.getOwnerUserId() != null, Customer::getOwnerUserId, q.getOwnerUserId());
                break;
            default:
                // all: 仅管理员/主管可看全部, 业务员看本人+公海
                if (!user.getRoles().contains("ROLE_ADMIN") && !user.getRoles().contains("ROLE_MANAGER")) {
                    wrap.and(w -> w.eq(Customer::getOwnerUserId, user.getUserId())
                            .or().isNull(Customer::getOwnerUserId));
                }
        }
        Page<Customer> page = customerMapper.selectPage(new Page<>(q.getCurrent(), q.getSize()), wrap);
        Page<CustomerVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<CustomerVO> vos = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        fillOwnerNames(vos);
        result.setRecords(vos);
        return result;
    }

    public Customer detail(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return c;
    }

    /** 认领公海客户 */
    @Transactional
    public void claim(Long customerId) {
        Customer c = mustGet(customerId);
        SecurityUtils.LoginUser u = SecurityUtils.current();
        if (c.getOwnerUserId() != null) {
            throw new BusinessException(ResultCode.CUSTOMER_OWNED);
        }
        c.setOwnerUserId(u.getUserId());
        c.setPoolStatus(2);
        customerMapper.updateById(c);
        writePoolLog(customerId, "CLAIM", null, u.getUserId(), "业务员认领");
    }

    /** 退回公海 */
    @Transactional
    public void release(Long customerId, String remark) {
        Customer c = mustGet(customerId);
        SecurityUtils.LoginUser u = SecurityUtils.current();
        if (!u.getRoles().contains("ROLE_ADMIN") && !u.getRoles().contains("ROLE_MANAGER")
                && !u.getUserId().equals(c.getOwnerUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        writePoolLog(customerId, "RELEASE", c.getOwnerUserId(), null, remark);
        c.setOwnerUserId(null);
        c.setPoolStatus(1);
        customerMapper.updateById(c);
    }

    /** 主管分配/转交给指定业务员 */
    @Transactional
    public void transfer(Long customerId, Long toUserId, String remark) {
        Customer c = mustGet(customerId);
        if (userMapper.selectById(toUserId) == null) {
            throw new BusinessException("目标业务员不存在");
        }
        writePoolLog(customerId, "TRANSFER", c.getOwnerUserId(), toUserId, remark);
        c.setOwnerUserId(toUserId);
        c.setPoolStatus(2);
        customerMapper.updateById(c);
    }

    public List<CustomerPoolLog> poolLogs(Long customerId) {
        return poolLogMapper.selectList(Wrappers.<CustomerPoolLog>lambdaQuery()
                .eq(CustomerPoolLog::getCustomerId, customerId)
                .orderByDesc(CustomerPoolLog::getId));
    }

    /** 停用/拉黑 或恢复 */
    @Transactional
    public void toggleStatus(Long customerId, int status) {
        Customer c = mustGet(customerId);
        c.setStatus(status);
        customerMapper.updateById(c);
    }

    private Customer mustGet(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return c;
    }

    private void writePoolLog(Long customerId, String action, Long from, Long to, String remark) {
        SecurityUtils.LoginUser u = SecurityUtils.currentOrNull();
        CustomerPoolLog log = new CustomerPoolLog();
        log.setCustomerId(customerId);
        log.setAction(action);
        log.setFromUserId(from);
        log.setToUserId(to);
        log.setOperatorId(u == null ? null : u.getUserId());
        log.setRemark(remark);
        poolLogMapper.insert(log);
    }

    private CustomerVO toVO(Customer c) {
        CustomerVO vo = new CustomerVO();
        org.springframework.beans.BeanUtils.copyProperties(c, vo);
        return vo;
    }

    private void fillOwnerNames(List<CustomerVO> vos) {
        Set<Long> ids = vos.stream().map(Customer::getOwnerUserId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        vos.forEach(vo -> vo.setOwnerName(vo.getOwnerUserId() == null
                ? "公海客户" : names.getOrDefault(vo.getOwnerUserId(), "已离职")));
    }
}
