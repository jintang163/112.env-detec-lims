package com.lims.module.entrust.dto;

import com.lims.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntrustQueryDTO extends PageQuery {
    private Long customerId;
    private Long contractId;
    private String status;
    private String urgency;
    private String entrustType;
    private LocalDate expectDateStart;
    private LocalDate expectDateEnd;
    /** mine: 仅本人创建 */
    private Boolean mine;
}
