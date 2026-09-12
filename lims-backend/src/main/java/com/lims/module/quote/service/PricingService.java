package com.lims.module.quote.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.common.formula.FormulaEngine;
import com.lims.module.quote.entity.PricingRule;
import com.lims.module.quote.entity.QuoteItem;
import com.lims.module.quote.mapper.PricingRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报价计价: 依据检测项目名称命中计价规则(关键字 + 优先级), 用 Aviator 执行公式。
 * 变量: qty 数量/点次, unit_price 单价, points 点位数(=qty), complexity 工况系数(默认1),
 *       urgent_factor 加急系数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRuleMapper ruleMapper;
    private final FormulaEngine formulaEngine;

    private List<PricingRule> activeRules() {
        return ruleMapper.selectList(Wrappers.<PricingRule>lambdaQuery()
                        .eq(PricingRule::getStatus, 1))
                .stream()
                .sorted(Comparator.comparingInt(r -> r.getPriority() == null ? 100 : r.getPriority()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 计算单条明细金额, 返回命中的表达式(用于快照展示)
     */
    public PricingResult price(QuoteItem item, BigDecimal urgentFactor, BigDecimal complexity) {
        List<PricingRule> rules = activeRules();
        PricingRule hit = rules.stream()
                .filter(r -> r.getKeyword() != null && !r.getKeyword().isEmpty()
                        && item.getItemName() != null
                        && item.getItemName().contains(r.getKeyword()))
                .findFirst()
                .orElseGet(() -> rules.stream()
                        .filter(r -> r.getKeyword() == null || r.getKeyword().isEmpty())
                        .findFirst()
                        .orElse(null));
        String expression;
        if (hit == null) {
            // 兜底: 数量×单价
            expression = "qty * unit_price";
        } else {
            expression = hit.getExpression();
        }

        BigDecimal qty = nz(item.getQty());
        BigDecimal unitPrice = nz(item.getUnitPrice());
        Map<String, Object> vars = new HashMap<>();
        vars.put("qty", qty);
        vars.put("unit_price", unitPrice);
        vars.put("points", qty);
        vars.put("complexity", complexity == null ? BigDecimal.ONE : complexity);
        vars.put("urgent_factor", urgentFactor == null ? BigDecimal.ONE : urgentFactor);

        BigDecimal amount = formulaEngine.evalDecimal(expression, vars).setScale(2, RoundingMode.HALF_UP);
        return new PricingResult(amount, expression, hit == null ? "默认规则" : hit.getRuleName());
    }

    /** 校验规则表达式 */
    public void validateRule(String expression) {
        formulaEngine.validate(expression);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PricingResult {
        private BigDecimal amount;
        private String expression;
        private String ruleName;
    }
}
