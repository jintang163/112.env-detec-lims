package com.lims.module.sampling.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_sampling_plan_item")
public class SamplingPlanItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    /** 所属计划点位ID(空表示通用) */
    private Long pointId;
    private Long orderItemId;
    private String itemName;
    private String sampleName;
    private Integer sampleQty;
    /** 采样容器 */
    private String container;
    /** 保存条件(常温/冷藏/冷冻/避光/密封) */
    private String preservation;
    /** 是否需要质控样 */
    private Integer qcRequired;
    private Integer sortNo;
}
