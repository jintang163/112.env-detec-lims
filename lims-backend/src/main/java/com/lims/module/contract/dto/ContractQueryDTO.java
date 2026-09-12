package com.lims.module.contract.dto;

import com.lims.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContractQueryDTO extends PageQuery {
    private Long customerId;
    private String status;
    private LocalDate signDateStart;
    private LocalDate signDateEnd;
}
