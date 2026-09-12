package com.lims.module.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lims.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract")
public class Contract extends BaseEntity {
    private String code;
    private String name;
    private Long customerId;
    private Long quoteId;
    private BigDecimal amount;
    private BigDecimal taxRate;
    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String paymentMethod;
    private String paymentTerms;
    private String ourParty;
    private String counterParty;
    /** DRAFT/APPROVING/APPROVED/EXECUTING/COMPLETED/CHANGED/TERMINATED */
    private String status;
    private BigDecimal receivedAmount;
    private BigDecimal invoicedAmount;
    private Long fileId;
    private String fileUrl;
    private Long approvalId;
    private LocalDateTime approvedAt;
    private String remark;
}
