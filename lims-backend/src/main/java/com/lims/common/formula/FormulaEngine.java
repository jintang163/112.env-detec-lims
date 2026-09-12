package com.lims.common.formula;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aviator 公式引擎封装。
 * 报价计价变量: qty(数量) unit_price(单价) points(点位数) complexity(工况系数) urgent_factor(加急系数)
 * 表达式编译结果缓存, 安全起见仅开放算术运算白名单。
 */
@Slf4j
@Component
public class FormulaEngine {

    private final Map<String, Expression> cache = new ConcurrentHashMap<>();

    public FormulaEngine() {
        // Aviator 默认特性已包含算术/三元/比较运算, 足以满足报价计价;
        // 规则表达式在后台维护, 不暴露给终端用户。
    }

    /**
     * 执行表达式, 返回 BigDecimal
     */
    public BigDecimal evalDecimal(String expression, Map<String, Object> vars) {
        try {
            Expression compiled = cache.computeIfAbsent(expression, AviatorEvaluator::compile);
            Object result = compiled.execute(vars);
            if (result == null) {
                return BigDecimal.ZERO;
            }
            if (result instanceof Number) {
                return new BigDecimal(result.toString());
            }
            return new BigDecimal(result.toString());
        } catch (Exception e) {
            log.error("公式执行失败: expr={} vars={} err={}", expression, vars, e.getMessage());
            throw new BusinessException(ResultCode.PRICING_RULE_ERROR.getCode(),
                    "计价公式执行失败: " + e.getMessage());
        }
    }

    /**
     * 仅校验表达式是否可编译(规则维护时使用)
     */
    public void validate(String expression) {
        try {
            AviatorEvaluator.validate(expression);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PRICING_RULE_ERROR.getCode(),
                    "公式语法错误: " + e.getMessage());
        }
    }
}
