package com.lims.module.entrust.dto;

import com.lims.module.entrust.entity.EntrustItem;
import com.lims.module.entrust.entity.EntrustOrder;
import com.lims.module.entrust.entity.EntrustSamplingPoint;
import lombok.Data;

import java.util.List;

@Data
public class EntrustDetailVO {
    private EntrustOrder order;
    private String customerName;
    private String customerLevel;
    private String contractCode;
    private String contractName;
    private String quoteCode;
    private String reviewerName;
    private List<EntrustItem> items;
    private List<EntrustSamplingPoint> points;
}
