package com.lims.module.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_customer_credit_log")
public class CustomerCreditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    /** 1信用分 2授信额度 3账期 4冻结/解冻 */
    private Integer changeType;
    private String beforeValue;
    private String afterValue;
    private String reason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;
}
