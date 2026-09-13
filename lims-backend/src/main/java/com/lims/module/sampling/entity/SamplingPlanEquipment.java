package com.lims.module.sampling.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_sampling_plan_equipment")
public class SamplingPlanEquipment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long equipmentId;
    /** 设备名称快照 */
    private String equipmentName;
    private Integer qty;
}
