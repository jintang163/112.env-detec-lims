package com.lims.module.contract.dto;

import com.lims.module.contract.entity.Contract;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContractVO extends Contract {
    private String customerName;
    private String customerLevel;
    private String statusLabel;
}
