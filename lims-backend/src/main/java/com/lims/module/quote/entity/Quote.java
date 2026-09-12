package com.lims.module.quote.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lims.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_quote")
public class Quote extends BaseEntity {
    private String code;
    private String title;
    private Long customerId;
    /** RULE/MANUAL */
    private String pricingMode;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal urgentFactor;
    private LocalDate validUntil;
    /** DRAFT/APPROVING/APPROVED/REJECTED/VOID */
    private String status;
    private Long approvalId;
    private LocalDateTime approvedAt;
    private String fileUrl;
    private String remark;
}
