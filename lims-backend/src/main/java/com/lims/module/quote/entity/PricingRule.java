package com.lims.module.quote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_quote_pricing_rule")
public class PricingRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    private String keyword;
    private String expression;
    private Integer priority;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
