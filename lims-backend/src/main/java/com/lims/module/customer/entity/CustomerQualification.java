package com.lims.module.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_customer_qualification")
public class CustomerQualification {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long customerId;
    private String qualType;
    private String certNo;
    private Long fileId;
    private String fileName;
    private String fileUrl;
    private LocalDate validFrom;
    private LocalDate validTo;
    /** 1有效 2即将过期 3已过期 */
    private Integer validStatus;
    private LocalDateTime createTime;
}
