package com.lims.common.core;

import lombok.Data;

@Data
public class PageQuery {

    /** 当前页 */
    private long current = 1;
    /** 每页条数 */
    private long size = 10;
    /** 通用关键字 */
    private String keyword;
    /** 排序字段 */
    private String orderField;
    /** asc/desc */
    private String orderDir = "asc";
}
