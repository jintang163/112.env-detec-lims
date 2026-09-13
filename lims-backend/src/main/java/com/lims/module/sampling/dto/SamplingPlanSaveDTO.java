package com.lims.module.sampling.dto;

import com.lims.module.sampling.entity.SamplingPlanEquipment;
import com.lims.module.sampling.entity.SamplingPlanItem;
import com.lims.module.sampling.entity.SamplingPlanPoint;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SamplingPlanSaveDTO {
    private Long id;
    @NotNull(message = "请选择委托单")
    private Long orderId;
    private String title;
    @NotNull(message = "请选择采样日期")
    private LocalDate planDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String weather;
    private String remark;
    /** 不传则按委托单现有 点位/检测项 自动生成草稿明细 */
    @Valid
    private List<SamplingPlanPoint> points;
    @Valid
    private List<SamplingPlanItem> items;
    @Valid
    private List<SamplingPlanEquipment> equipments;
}
