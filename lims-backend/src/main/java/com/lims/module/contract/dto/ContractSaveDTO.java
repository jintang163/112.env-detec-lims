package com.lims.module.contract.dto;

import com.lims.module.contract.entity.Contract;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractSaveDTO {
    private Long id;
    @NotBlank(message = "合同名称不能为空")
    private String name;
    @NotNull(message = "请选择客户")
    private Long customerId;
    private Long quoteId;
    @NotNull(message = "合同金额不能为空")
    private BigDecimal amount;
    private BigDecimal taxRate;
    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String paymentMethod;
    private String paymentTerms;
    private String ourParty;
    private String counterParty;
    private Long fileId;
    private String fileUrl;
    private String remark;

    public Contract toEntity() {
        Contract c = new Contract();
        org.springframework.beans.BeanUtils.copyProperties(this, c);
        return c;
    }
}
