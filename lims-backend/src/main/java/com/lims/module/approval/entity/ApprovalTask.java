package com.lims.module.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_approval_task")
public class ApprovalTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Integer nodeSeq;
    private String nodeName;
    private String roleCode;
    private Long assigneeId;
    private String assigneeName;
    /** PENDING/APPROVED/REJECTED */
    private String status;
    private String comment;
    private LocalDateTime actedAt;
    private LocalDateTime createTime;
}
