package com.lims.module.quote.dto;

import com.lims.module.quote.entity.Quote;
import com.lims.module.quote.entity.QuoteItem;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class QuoteSaveDTO {
    private Long id;
    @NotBlank(message = "报价标题不能为空")
    private String title;
    @NotNull(message = "请选择客户")
    private Long customerId;
    private String pricingMode = "RULE";
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal urgentFactor = BigDecimal.ONE;
    private LocalDate validUntil;
    private String remark;
    @Valid
    private List<QuoteItem> items;
}
