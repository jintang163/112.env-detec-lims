package com.lims.module.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_contract_change")
public class ContractChange {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long contractId;
    private String changeNo;
    /** AMOUNT/PERIOD/TERMS/OTHER */
    private String changeType;
    private String beforeContent;
    private String afterContent;
    private String reason;
    /** APPROVING/APPROVED/REJECTED */
    private String status;
    private Long approvalId;
    private String createBy;
    private LocalDateTime createTime;
}
