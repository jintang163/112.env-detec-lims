package com.lims.module.sampling.dto;

import com.lims.module.sampling.entity.SampleHandover;
import lombok.Data;

import java.util.List;

@Data
public class HandoverDetailVO {
    private SampleHandover handover;
    private String taskCode;
    private String planTitle;
    private String sigUrl;
    private List<SampleVO> samples;
}
