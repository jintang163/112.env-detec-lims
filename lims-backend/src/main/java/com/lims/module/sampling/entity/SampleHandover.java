package com.lims.module.sampling.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lims.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_sample_handover")
public class SampleHandover extends BaseEntity {
    /** 交接单编号 JJyyyy-xxxx */
    private String code;
    private Long taskId;
    private Long planId;
    private Long orderId;
    private Integer sampleCount;
    /** 样品状态描述(完好/异常说明) */
    private String sampleStatus;
    private Long handoverById;
    private String handoverByName;
    private LocalDateTime handoverAt;
    /** 采样员签名文件ID(sys_file) */
    private Long sigFileId;
    private Long receiverId;
    private String receiverName;
    private LocalDateTime receiveAt;
    /** 接收备注/拒收原因 */
    private String receiverRemark;
    /** PENDING待接收/CONFIRMED已接收/REJECTED已拒收 */
    private String status;

    @TableField(exist = false)
    private String taskCode;
    @TableField(exist = false)
    private String planTitle;
    @TableField(exist = false)
    private String orderCode;
}
