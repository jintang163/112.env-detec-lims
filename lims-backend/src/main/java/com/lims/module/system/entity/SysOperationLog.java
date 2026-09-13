package com.lims.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志(对应 sys_operation_log, 由 OperationLogAspect 自动写入) */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String method;
    private String params;
    private String ip;
    private Integer costMs;
    private LocalDateTime createTime;
}
