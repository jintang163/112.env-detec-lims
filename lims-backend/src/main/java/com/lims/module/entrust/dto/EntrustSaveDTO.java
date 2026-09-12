package com.lims.module.entrust.dto;

import com.lims.module.entrust.entity.EntrustItem;
import com.lims.module.entrust.entity.EntrustOrder;
import com.lims.module.entrust.entity.EntrustSamplingPoint;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class EntrustSaveDTO {
    private Long id;
    @NotBlank(message = "项目名称不能为空")
    private String title;
    @NotNull(message = "请选择客户")
    private Long customerId;
    private Long contractId;
    private Long quoteId;
    private String entrustType = "ENTRUST";
    private String urgency = "NORMAL";
    private Integer sampleSource = 1;
    private String contactPerson;
    private String contactPhone;
    private String province;
    private String city;
    private String district;
    private String samplingAddress;
    private java.math.BigDecimal lng;
    private java.math.BigDecimal lat;
    private java.time.LocalDateTime plannedSamplingTime;
    private java.time.LocalDate expectedReportDate;
    private String remark;
    @Valid
    private List<EntrustItem> items;
    @Valid
    private List<EntrustSamplingPoint> points;

    public EntrustOrder toEntity() {
        EntrustOrder o = new EntrustOrder();
        org.springframework.beans.BeanUtils.copyProperties(this, o, "items", "points");
        return o;
    }
}
