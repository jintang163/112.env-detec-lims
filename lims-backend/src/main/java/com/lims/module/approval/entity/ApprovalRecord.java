package com.lims.module.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_approval_record")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Integer nodeSeq;
    private String nodeName;
    private Long approverId;
    private String approverName;
    /** APPROVE/REJECT */
    private String action;
    private String comment;
    private LocalDateTime createTime;
}
