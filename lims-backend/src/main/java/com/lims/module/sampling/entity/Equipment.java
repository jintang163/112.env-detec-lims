package com.lims.module.sampling.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lims.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_equipment")
public class Equipment extends BaseEntity {
    /** 设备编号 SByyyy-xxxx */
    private String code;
    private String name;
    /** DEVICE设备/CONTAINER容器 */
    private String category;
    private String spec;
    private String unit;
    private Integer qtyTotal;
    private Integer qtyAvailable;
    /** NORMAL正常/MAINTENANCE维修中/SCRAPPED报废 */
    private String status;
    private Long keeperId;
    private String keeperName;
    private LocalDate purchaseDate;
    private String remark;
}
