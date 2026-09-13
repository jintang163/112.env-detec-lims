package com.lims.module.sampling.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("biz_sampling_plan_point")
public class SamplingPlanPoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    /** 关联委托点位ID(现场新增可空) */
    private Long pointId;
    private String name;
    /** BD-09 */
    private BigDecimal lng;
    private BigDecimal lat;
    private String addrDesc;
    private Integer sortNo;
}
