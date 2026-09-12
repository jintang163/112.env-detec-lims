package com.lims.module.entrust.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_entrust_subcontract")
public class EntrustSubcontract {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    /** 空=整单分包 */
    private Long itemId;
    private String subcontractor;
    private String qualCert;
    private String qualFileUrl;
    private BigDecimal amount;
    private String reason;
    /** APPROVING/APPROVED/REJECTED */
    private String status;
    private Long approvalId;
    private String createBy;
    private LocalDateTime createTime;
}
