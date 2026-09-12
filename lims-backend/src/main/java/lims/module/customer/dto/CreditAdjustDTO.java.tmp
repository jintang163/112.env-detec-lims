package com.lims.module.customer.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class CreditAdjustDTO {
    @NotNull
    private Long customerId;
    /** 1信用分 2授信额度 3账期 */
    @NotNull
    private Integer changeType;
    /** 目标值(信用分0-100/授信额度/账期天数) */
    @NotNull
    private BigDecimal targetValue;
    @NotNull(message = "请填写调整原因")
    private String reason;
}
