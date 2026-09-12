package com.lims.module.quote.dto;

import com.lims.module.quote.entity.Quote;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class QuoteVO extends Quote {
    private String customerName;
    private String customerLevel;
    private List<com.lims.module.quote.entity.QuoteItem> items;
}
