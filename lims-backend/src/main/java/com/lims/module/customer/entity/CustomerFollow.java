package com.lims.module.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_customer_follow")
public class CustomerFollow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String content;
    private String followType;
    private LocalDateTime nextTime;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;
}
