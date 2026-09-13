package com.lims.module.sampling.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class CheckoutDTO {
    @NotNull(message = "请选择设备")
    private Long equipmentId;
    @NotNull
    @Min(value = 1, message = "领用数量至少为1")
    private Integer qty;
    private Long planId;
    private Long taskId;
    private LocalDateTime expectedReturnTime;
    private String checkoutRemark;
}
