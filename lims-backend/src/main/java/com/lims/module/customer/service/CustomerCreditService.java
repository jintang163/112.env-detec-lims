package com.lims.module.customer.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.common.security.SecurityUtils;
import com.lims.module.customer.dto.CreditAdjustDTO;
import com.lims.module.customer.entity.Customer;
import com.lims.module.customer.entity.CustomerCreditLog;
import com.lims.module.customer.mapper.CustomerCreditLogMapper;
import com.lims.module.customer.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerCreditService {

    private final CustomerMapper customerMapper;
    private final CustomerCreditLogMapper creditLogMapper;

    @PreAuthorize("hasAuthority('PERM_customer:credit')")
    @Transactional
    public void adjust(CreditAdjustDTO dto) {
        Customer c = customerMapper.selectById(dto.getCustomerId());
        if (c == null) {
            return;
        }
        BigDecimal target = dto.getTargetValue();
        String before;
        String after = target.stripTrailingZeros().toPlainString();
        switch (dto.getChangeType()) {
            case 1:
                if (target.intValue() < 0 || target.intValue() > 100) {
                    throw new IllegalArgumentException("信用分必须在0-100之间");
                }
                before = String.valueOf(c.getCreditScore());
                c.setCreditScore(target.intValue());
                // 信用分联动分级: >=85 A, >=70 B, >=50 C, 否则 D
                c.setCustomerLevel(target.intValue() >= 85 ? "A"
                        : target.intValue() >= 70 ? "B"
                        : target.intValue() >= 50 ? "C" : "D");
                break;
            case 2:
                if (target.signum() < 0) {
                    throw new IllegalArgumentException("授信额度不能为负");
                }
                before = String.valueOf(c.getCreditLimit());
                c.setCreditLimit(target);
                break;
            case 3:
                if (target.intValue() < 0) {
                    throw new IllegalArgumentException("账期不能为负");
                }
                before = String.valueOf(c.getCreditPeriod());
                c.setCreditPeriod(target.intValue());
                break;
            default:
                throw new IllegalArgumentException("未知的信用变更类型");
        }
        customerMapper.updateById(c);

        SecurityUtils.LoginUser u = SecurityUtils.current();
        CustomerCreditLog log = new CustomerCreditLog();
        log.setCustomerId(c.getId());
        log.setChangeType(dto.getChangeType());
        log.setBeforeValue(before);
        log.setAfterValue(after);
        log.setReason(dto.getReason());
        log.setOperatorId(u.getUserId());
        log.setOperatorName(u.getRealName());
        creditLogMapper.insert(log);
    }

    public List<CustomerCreditLog> logs(Long customerId) {
        return creditLogMapper.selectList(Wrappers.<CustomerCreditLog>lambdaQuery()
                .eq(CustomerCreditLog::getCustomerId, customerId)
                .orderByDesc(CustomerCreditLog::getId));
    }
}
