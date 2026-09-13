package com.lims.module.sampling.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 移动端现场样品提交(支持离线幂等: clientUuid 由端上生成)
 */
@Data
public class SampleSubmitDTO {
    @NotBlank(message = "clientUuid不能为空")
    private String clientUuid;
    @NotNull(message = "缺少采样任务")
    private Long taskId;
    private String sampleCode;
    private Long pointId;
    private String pointName;
    private Long entrustItemId;
    private String itemName;
    private String sampleName;
    private LocalDateTime samplingTime;
    private BigDecimal lng;
    private BigDecimal lat;
    private String addrDesc;
    private BigDecimal temperature;
    private BigDecimal ph;
    /** 其他现场参数键值, 落 params_json */
    private Map<String, String> params;
    private String container;
    private String storageCondition;
    private Integer isQc;
    /** BLANK/PARALLEL/SPIKE */
    private String qcType;
    private String remark;
    /** 已上传照片的文件ID, 提交后晚绑定到样品 */
    private List<Long> photoFileIds;
}
