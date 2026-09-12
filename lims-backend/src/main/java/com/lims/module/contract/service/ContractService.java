package com.lims.module.contract.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.common.util.CodeGenerator;
import com.lims.module.approval.service.ApprovalService;
import com.lims.module.contract.dto.ContractQueryDTO;
import com.lims.module.contract.dto.ContractSaveDTO;
import com.lims.module.contract.dto.ContractVO;
import com.lims.module.contract.entity.Contract;
import com.lims.module.contract.entity.ContractPayment;
import com.lims.module.contract.mapper.ContractMapper;
import com.lims.module.contract.mapper.ContractPaymentMapper;
import com.lims.module.customer.entity.Customer;
import com.lims.module.customer.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService {

    public static final String BIZ_TYPE = "CONTRACT";

    private final ContractMapper contractMapper;
    private final ContractPaymentMapper paymentMapper;
    private final CustomerMapper customerMapper;
    private final ApprovalService approvalService;
    private final CodeGenerator codeGenerator;

    @Transactional
    public Long save(ContractSaveDTO dto) {
        Contract c = dto.getId() == null ? new Contract() : contractMapper.selectById(dto.getId());
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (dto.getId() != null && !"DRAFT".equals(c.getStatus()) && !"REJECTED".equals(c.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "仅草稿/被驳回的合同可编辑");
        }
        org.springframework.beans.BeanUtils.copyProperties(dto, c, "id");
        if (dto.getId() == null) {
            c.setCode(codeGenerator.next("HT", false));
            c.setStatus("DRAFT");
            c.setReceivedAmount(BigDecimal.ZERO);
            c.setInvoicedAmount(BigDecimal.ZERO);
            contractMapper.insert(c);
        } else {
            contractMapper.updateById(c);
        }
        return c.getId();
    }

    @Transactional
    public void submit(Long id) {
        Contract c = mustGet(id);
        if (!"DRAFT".equals(c.getStatus()) && !"REJECTED".equals(c.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED);
        }
        c.setStatus("APPROVING");
        contractMapper.updateById(c);
        SecurityUtils.LoginUser u = SecurityUtils.current();
        Long instId = approvalService.start(BIZ_TYPE, id, "合同审批: " + c.getName() + "(" + c.getCode() + ")",
                u.getUserId(), u.getRealName());
        c.setApprovalId(instId);
        contractMapper.updateById(c);
    }

    /** 审批通过: APPROVING -> APPROVED(待履约启动), 并同步进入 EXECUTING 由首次收款/委托触发;
     *  这里直接置为 APPROVED, 业务可在创建委托单时启动履约。 */
    @Transactional
    public void onApproved(Long id, Long instanceId) {
        Contract c = mustGet(id);
        c.setStatus("APPROVED");
        c.setApprovedAt(java.time.LocalDateTime.now());
        contractMapper.updateById(c);
    }

    @Transactional
    public void onRejected(Long id, Long instanceId, String comment) {
        Contract c = mustGet(id);
        c.setStatus("DRAFT");
        contractMapper.updateById(c);
    }

    /** 开始履约 / 完成 / 终止 */
    @Transactional
    public void changeStatus(Long id, String target, String remark) {
        Contract c = mustGet(id);
        String s = c.getStatus();
        boolean ok;
        switch (target) {
            case "EXECUTING":
                ok = "APPROVED".equals(s) || "CHANGED".equals(s);
                break;
            case "COMPLETED":
                ok = "EXECUTING".equals(s);
                break;
            case "TERMINATED":
                ok = "APPROVED".equals(s) || "EXECUTING".equals(s) || "CHANGED".equals(s);
                break;
            default:
                ok = false;
        }
        if (!ok) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(),
                    "当前状态[" + s + "]不允许变更为[" + target + "]");
        }
        c.setStatus(target);
        contractMapper.updateById(c);
    }

    public Contract detail(Long id) {
        return mustGet(id);
    }

    public Page<ContractVO> page(ContractQueryDTO q) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract> wrap =
                Wrappers.<Contract>lambdaQuery()
                .and(StringUtils.hasText(q.getKeyword()),
                        w -> w.like(Contract::getName, q.getKeyword())
                                .or().like(Contract::getCode, q.getKeyword()))
                .eq(q.getCustomerId() != null, Contract::getCustomerId, q.getCustomerId())
                .eq(StringUtils.hasText(q.getStatus()), Contract::getStatus, q.getStatus())
                .ge(q.getSignDateStart() != null, Contract::getSignDate, q.getSignDateStart())
                .le(q.getSignDateEnd() != null, Contract::getSignDate, q.getSignDateEnd())
                .orderByDesc(Contract::getId);
        Page<Contract> page = contractMapper.selectPage(new Page<>(q.getCurrent(), q.getSize()), wrap);
        Page<ContractVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ContractVO> vos = page.getRecords().stream().map(c -> {
            ContractVO vo = new ContractVO();
            org.springframework.beans.BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());
        fillCustomer(vos);
        result.setRecords(vos);
        return result;
    }

    /** 履约看板统计 */
    public Map<String, Object> performance(Long id) {
        Contract c = mustGet(id);
        BigDecimal received = c.getReceivedAmount() == null ? BigDecimal.ZERO : c.getReceivedAmount();
        BigDecimal invoiced = c.getInvoicedAmount() == null ? BigDecimal.ZERO : c.getInvoicedAmount();
        BigDecimal amount = c.getAmount() == null ? BigDecimal.ZERO : c.getAmount();
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("amount", amount);
        m.put("received", received);
        m.put("invoiced", invoiced);
        m.put("unreceived", amount.subtract(received).max(BigDecimal.ZERO));
        m.put("receiveRate", amount.signum() == 0 ? 0
                : received.multiply(new BigDecimal("100")).divide(amount, 1, java.math.RoundingMode.HALF_UP));
        m.put("uninvoiced", amount.subtract(invoiced).max(BigDecimal.ZERO));
        return m;
    }

    /** 收款/开票后回写累计金额 */
    @Transactional
    public void recalcAmounts(Long contractId) {
        BigDecimal recv = sumPayment(contractId, 2);
        BigDecimal inv = sumPayment(contractId, 3);
        Contract c = mustGet(contractId);
        c.setReceivedAmount(recv);
        c.setInvoicedAmount(inv);
        contractMapper.updateById(c);
    }

    private BigDecimal sumPayment(Long contractId, int payType) {
        return paymentMapper.selectList(Wrappers.<ContractPayment>lambdaQuery()
                        .eq(ContractPayment::getContractId, contractId)
                        .eq(ContractPayment::getPayType, payType))
                .stream()
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Contract mustGet(Long id) {
        Contract c = contractMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return c;
    }

    private void fillCustomer(List<ContractVO> vos) {
        Set<Long> ids = vos.stream().map(Contract::getCustomerId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, Customer> map = customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, x -> x));
        vos.forEach(vo -> {
            Customer c = map.get(vo.getCustomerId());
            if (c != null) {
                vo.setCustomerName(c.getName());
                vo.setCustomerLevel(c.getCustomerLevel());
            }
        });
    }
}
