package com.lims.module.entrust.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("biz_entrust_item")
public class EntrustItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String itemName;
    private String standardCode;
    private String standardName;
    private String sampleName;
    private Integer sampleQty;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private Integer isSubcontract;
    private String limitValue;
    private Integer sortNo;
    private String remark;
}
