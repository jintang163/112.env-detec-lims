package com.lims.module.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_approval_instance")
public class ApprovalInstance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private Long bizId;
    private String title;
    /** PENDING/APPROVED/REJECTED */
    private String status;
    private Integer currentSeq;
    private Long initiatorId;
    private String initiatorName;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
