package com.lims.module.customer.dto;

import com.lims.module.customer.entity.Customer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerVO extends Customer {
    private String ownerName;
    private String deptName;
    /** 未结清合同额等(看板用,可选) */
    private java.math.BigDecimal ongoingAmount;
}
