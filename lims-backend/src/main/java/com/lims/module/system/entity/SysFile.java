package com.lims.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFile {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String bizType;
    private Long bizId;
    private String originalName;
    private String objectName;
    private String url;
    private String contentType;
    private Long fileSize;
    private String storageType;
    private Long uploaderId;
    private LocalDateTime createTime;
}
