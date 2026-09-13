package com.lims.module.sampling.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class HandoverCreateDTO {
    @NotNull(message = "缺少采样任务")
    private Long taskId;
    /** 样品状态描述(完好/异常说明) */
    private String sampleStatus;
    /** 采样员手写签名文件ID */
    private Long sigFileId;
    private String remark;
}
