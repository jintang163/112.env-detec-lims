package com.lims.module.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_contract_payment")
public class ContractPayment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    /** 1收款计划 2实际收款 3开票记录 */
    private Integer payType;
    private LocalDate planDate;
    private LocalDate occurDate;
    private BigDecimal amount;
    private String invoiceNo;
    private String remark;
    private LocalDateTime createTime;
}
