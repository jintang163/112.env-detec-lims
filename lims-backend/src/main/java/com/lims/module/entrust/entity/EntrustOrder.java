package com.lims.module.entrust.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lims.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_entrust_order")
public class EntrustOrder extends BaseEntity {
    private String code;
    private String title;
    private Long customerId;
    private Long contractId;
    private Long quoteId;
    /** ENTRUST/SUPERVISION/SPOT */
    private String entrustType;
    /** NORMAL/URGENT */
    private String urgency;
    /** DRAFT/REVIEWING/REVIEW_REJECTED/ACCEPTED/SAMPLING/TESTING/REPORTING/COMPLETED/CANCELLED */
    private String status;
    /** 1现场采样 2客户送样 */
    private Integer sampleSource;
    private String contactPerson;
    private String contactPhone;
    private String province;
    private String city;
    private String district;
    private String samplingAddress;
    /** BD-09 */
    private BigDecimal lng;
    private BigDecimal lat;
    private LocalDateTime plannedSamplingTime;
    private LocalDate expectedReportDate;
    private BigDecimal totalAmount;
    private BigDecimal adjustedAmount;
    private String adjustmentReason;
    private Integer hasSubcontract;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String reviewOpinion;
    private LocalDateTime acceptedAt;
    private LocalDateTime samplingAt;
    private LocalDateTime testingAt;
    private LocalDateTime reportingAt;
    private LocalDateTime completedAt;
    private String cancelReason;
    private Long approvalId;
    private String remark;
}
