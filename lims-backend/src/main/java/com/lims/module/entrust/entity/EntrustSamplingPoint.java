package com.lims.module.entrust.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("biz_entrust_sampling_point")
public class EntrustSamplingPoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String name;
    /** BD-09 经纬度 */
    private BigDecimal lng;
    private BigDecimal lat;
    private String addrDesc;
    private Integer sortNo;
}
