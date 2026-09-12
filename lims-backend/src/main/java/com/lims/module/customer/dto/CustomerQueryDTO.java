package com.lims.module.customer.dto;

import com.lims.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerQueryDTO extends PageQuery {
    /** pool 公海 / mine 我的客户 / all 全部 */
    private String scope = "all";
    private String customerLevel;
    private Integer customerType;
    private Integer poolStatus;
    private Integer status;
    private Long ownerUserId;
}
