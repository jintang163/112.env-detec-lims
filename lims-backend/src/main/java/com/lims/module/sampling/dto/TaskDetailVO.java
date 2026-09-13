package com.lims.module.sampling.dto;

import com.lims.module.sampling.entity.*;
import lombok.Data;

import java.util.List;

@Data
public class TaskDetailVO {
    private SamplingTask task;
    private SamplingPlan plan;
    private String orderCode;
    private String orderTitle;
    private List<SamplingPlanPoint> points;
    private List<SamplingPlanItem> items;
    private List<SamplingPlanEquipment> equipments;
    private List<SampleVO> samples;
}
