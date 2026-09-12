package com.lims.module.customer.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class CustomerSaveDTO {
    private Long id;
    @NotBlank(message = "客户名称不能为空")
    private String name;
    private String shortName;
    private Integer customerType = 10;
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
    private String customerLevel = "C";
    private BigDecimal creditLimit = BigDecimal.ZERO;
    private Integer creditPeriod = 0;
    private String source;
    /** 新建时: true 直接进公海(默认), false 归当前业务员 */
    private Boolean toPool = Boolean.TRUE;
    private String remark;
}
