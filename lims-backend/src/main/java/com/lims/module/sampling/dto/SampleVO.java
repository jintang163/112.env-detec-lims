package com.lims.module.sampling.dto;

import com.lims.module.sampling.entity.FieldSample;
import com.lims.module.system.entity.SysFile;
import lombok.Data;

import java.util.List;

@Data
public class SampleVO {
    private FieldSample sample;
    private List<SysFile> photos;
}
