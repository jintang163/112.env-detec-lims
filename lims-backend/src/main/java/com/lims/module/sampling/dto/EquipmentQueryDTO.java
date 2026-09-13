package com.lims.module.sampling.dto;

import com.lims.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentQueryDTO extends PageQuery {
    private String category;
    private String status;
}
