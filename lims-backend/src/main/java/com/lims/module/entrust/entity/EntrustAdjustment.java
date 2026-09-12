package com.lims.module.entrust.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_entrust_adjustment")
public class EntrustAdjustment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    /** 1调增 2调减 */
    private Integer adjustType;
    private BigDecimal beforeAmount;
    private BigDecimal adjustAmount;
    private BigDecimal afterAmount;
    private String reason;
    /** APPROVING/APPROVED/REJECTED */
    private String status;
    private Long approvalId;
    private String createBy;
    private LocalDateTime createTime;
}
