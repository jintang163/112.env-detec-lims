package com.lims.module.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lims.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_customer")
public class Customer extends BaseEntity {
    private String code;
    private String name;
    private String shortName;
    /** 10企业 20政府事业 30个人 */
    private Integer customerType;
    private String industry;
    private String contactPerson;
    private String contactPhone;
    private String email;
    private String province;
    private String city;
    private String district;
    private String address;
    private String bankName;
    private String bankAccount;
    private String taxNo;
    /** A/B/C/D */
    private String customerLevel;
    private Integer creditScore;
    private BigDecimal creditLimit;
    private Integer creditPeriod;
    private String source;
    /** NULL=公海 */
    private Long ownerUserId;
    /** 1公海 2已认领 3冻结 */
    private Integer poolStatus;
    private Integer followCount;
    private LocalDateTime lastFollowAt;
    /** 1正常 0停用/黑名单 */
    private Integer status;
    private String remark;
}
