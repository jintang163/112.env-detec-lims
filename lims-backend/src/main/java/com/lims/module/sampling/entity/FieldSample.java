package com.lims.module.sampling.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lims.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_field_sample")
public class FieldSample extends BaseEntity {
    /** 样品编号/二维码内容(任务号-序号) */
    private String sampleCode;
    /** 端上UUID, 离线提交幂等键 */
    private String clientUuid;
    private Long taskId;
    private Long planId;
    private Long orderId;
    private Long pointId;
    private String pointName;
    private Long entrustItemId;
    private String itemName;
    private String sampleName;
    private LocalDateTime samplingTime;
    /** 实际采样点 BD-09 */
    private BigDecimal lng;
    private BigDecimal lat;
    private String addrDesc;
    /** 现场温度(℃) */
    private BigDecimal temperature;
    /** 现场pH */
    private BigDecimal ph;
    /** 其他现场参数键值JSON */
    private String paramsJson;
    private String container;
    private String storageCondition;
    /** 是否质控样 */
    private Integer isQc;
    /** BLANK全程序空白/PARALLEL平行样/SPIKE加标样 */
    private String qcType;
    /** COLLECTED已采集/RECEIVED已接收 */
    private String status;
    private Long handoverId;
    private Long samplerId;
    private String samplerName;
    private String remark;
}
