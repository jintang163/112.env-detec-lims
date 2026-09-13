package com.lims.module.sampling.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class EquipmentReturnDTO {
    /** OK完好/DAMAGED损坏/MISSING缺失 */
    @NotBlank(message = "请选择归还检查结果")
    private String checkResult;
    private String returnRemark;
}
