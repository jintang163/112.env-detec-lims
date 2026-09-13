package com.lims.module.sampling.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lims.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_sampling_task")
public class SamplingTask extends BaseEntity {
    /** 任务编号 CYRWyyyy-xxxx(样品编号前缀) */
    private String code;
    private Long planId;
    private Long orderId;
    private Long assigneeId;
    private String assigneeName;
    private String assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime downloadedAt;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    /** ASSIGNED已分配/SUBMITTED已采样待交接/HANDED已交接/CANCELLED已取消 */
    private String status;
    private String remark;

    @TableField(exist = false)
    private String planTitle;
    @TableField(exist = false)
    private String orderCode;
    @TableField(exist = false)
    private Integer sampleCount;
}
