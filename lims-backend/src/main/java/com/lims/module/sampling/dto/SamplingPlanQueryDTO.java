package com.lims.module.sampling.dto;

import com.lims.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class SamplingPlanQueryDTO extends PageQuery {
    private Long orderId;
    private String status;
    private LocalDate planDateStart;
    private LocalDate planDateEnd;
}
