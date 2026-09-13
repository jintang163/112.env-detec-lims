package com.lims.module.sampling.dto;

import com.lims.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskQueryDTO extends PageQuery {
    private String status;
    private Long assigneeId;
    private Long planId;
    private Long orderId;
}
