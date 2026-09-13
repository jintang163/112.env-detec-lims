package com.lims.module.sampling.dto;

import com.lims.module.sampling.entity.*;
import lombok.Data;

import java.util.List;

@Data
public class PlanDetailVO {
    private SamplingPlan plan;
    private String orderCode;
    private String orderStatus;
    private List<SamplingPlanPoint> points;
    private List<SamplingPlanItem> items;
    private List<SamplingPlanEquipment> equipments;
    private List<SamplingTask> tasks;
}
