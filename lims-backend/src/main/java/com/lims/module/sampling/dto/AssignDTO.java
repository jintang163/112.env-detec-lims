package com.lims.module.sampling.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AssignDTO {
    @NotNull(message = "请选择采样计划")
    private Long planId;
    @NotNull(message = "请选择采样员")
    private Long assigneeId;
    private String remark;
}
