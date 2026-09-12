package com.lims.module.quote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("biz_quote_item")
public class QuoteItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long quoteId;
    private String itemName;
    private String standardCode;
    private String spec;
    private String unit;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String formula;
    private String remark;
    private Integer sortNo;
}
