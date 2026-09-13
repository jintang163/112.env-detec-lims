package com.lims.module.sampling.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lims.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_sampling_plan")
public class SamplingPlan extends BaseEntity {
    /** 计划编号 CYJHyyyy-xxxx */
    private String code;
    private Long orderId;
    private String title;
    private Long customerId;
    private String customerName;
    private LocalDate planDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String contactPerson;
    private String contactPhone;
    private String address;
    /** BD-09 */
    private BigDecimal lng;
    private BigDecimal lat;
    private String weather;
    /** DRAFT草稿/ISSUED已下发/CANCELLED已取消 */
    private String status;
    private String remark;

    @TableField(exist = false)
    private String orderCode;
}
