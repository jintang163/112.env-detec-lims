package com.lims.module.sampling.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_equipment_checkout")
public class EquipmentCheckout {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long equipmentId;
    /** 设备名称快照 */
    private String equipmentName;
    private Long planId;
    private Long taskId;
    private Integer qty;
    private Long checkoutById;
    private String checkoutByName;
    private LocalDateTime checkoutTime;
    private LocalDateTime expectedReturnTime;
    private String checkoutRemark;
    /** BORROWED已领用/RETURNED已归还 */
    private String status;
    private LocalDateTime returnTime;
    private Long returnById;
    private String returnByName;
    /** OK完好/DAMAGED损坏/MISSING缺失 */
    private String checkResult;
    private String returnRemark;
    private String createBy;
    private LocalDateTime createTime;
}
