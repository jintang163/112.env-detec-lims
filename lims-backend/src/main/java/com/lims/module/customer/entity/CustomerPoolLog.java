package com.lims.module.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_customer_pool_log")
public class CustomerPoolLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    /** CLAIM/RELEASE/TRANSFER */
    private String action;
    private Long fromUserId;
    private Long toUserId;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
