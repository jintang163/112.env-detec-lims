package com.lims.module.dashboard.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.common.core.Result;
import com.lims.common.security.SecurityUtils;
import com.lims.module.contract.entity.Contract;
import com.lims.module.contract.mapper.ContractMapper;
import com.lims.module.customer.entity.Customer;
import com.lims.module.customer.mapper.CustomerMapper;
import com.lims.module.customer.service.CustomerQualificationService;
import com.lims.module.entrust.entity.EntrustOrder;
import com.lims.module.entrust.mapper.EntrustOrderMapper;
import com.lims.module.quote.entity.Quote;
import com.lims.module.quote.mapper.QuoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台看板: 状态漏斗/客户/合同回款/报价/委托临期/资质预警
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final EntrustOrderMapper entrustMapper;
    private final CustomerMapper customerMapper;
    private final ContractMapper contractMapper;
    private final QuoteMapper quoteMapper;
    private final CustomerQualificationService qualificationService;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Map<String, Object> m = new HashMap<>();
        m.put("entrustTotal", entrustMapper.selectCount(Wrappers.<EntrustOrder>lambdaQuery()));
        m.put("entrustMonth", entrustMapper.countCurrentMonth());
        m.put("entrustUrgent", entrustMapper.selectCount(Wrappers.<EntrustOrder>lambdaQuery()
                .eq(EntrustOrder::getUrgency, "URGENT")
                .notIn(EntrustOrder::getStatus, "COMPLETED", "CANCELLED")));
        m.put("customerTotal", customerMapper.selectCount(Wrappers.<Customer>lambdaQuery()));
        m.put("poolTotal", customerMapper.selectCount(Wrappers.<Customer>lambdaQuery()
                .isNull(Customer::getOwnerUserId)));
        m.put("contractExecuting", contractMapper.selectCount(Wrappers.<Contract>lambdaQuery()
                .in(Contract::getStatus, "APPROVED", "EXECUTING", "CHANGED")));
        m.put("quotePending", quoteMapper.selectCount(Wrappers.<Quote>lambdaQuery()
                .eq(Quote::getStatus, "APPROVING")));

        // 回款汇总
        List<Contract> contracts = contractMapper.selectList(Wrappers.<Contract>lambdaQuery()
                .in(Contract::getStatus, "APPROVED", "EXECUTING", "CHANGED"));
        BigDecimal amount = BigDecimal.ZERO, received = BigDecimal.ZERO, invoiced = BigDecimal.ZERO;
        for (Contract c : contracts) {
            amount = amount.add(nz(c.getAmount()));
            received = received.add(nz(c.getReceivedAmount()));
            invoiced = invoiced.add(nz(c.getInvoicedAmount()));
        }
        Map<String, Object> payment = new HashMap<>();
        payment.put("amount", amount);
        payment.put("received", received);
        payment.put("invoiced", invoiced);
        payment.put("unreceived", amount.subtract(received).max(BigDecimal.ZERO));
        m.put("payment", payment);
        return Result.ok(m);
    }

    /** 委托单状态分布(ECharts 饼图/漏斗) */
    @GetMapping("/entrust-status")
    public Result<List<Map<String, Object>>> entrustStatus() {
        return Result.ok(entrustMapper.countByStatus());
    }

    /** 临期委托(期望报告日7天内) */
    @GetMapping("/expiring-entrusts")
    public Result<List<EntrustOrder>> expiring() {
        return Result.ok(entrustMapper.expiringSoon(7, 20));
    }

    /** 客户资质过期/临期预警 */
    @GetMapping("/qualification-warnings")
    public Result<?> qualificationWarnings() {
        SecurityUtils.current();
        return Result.ok(qualificationService.expiringSoon());
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
