package com.lims.module.quote.dto;

import com.lims.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QuoteQueryDTO extends PageQuery {
    private Long customerId;
    private String status;
}
