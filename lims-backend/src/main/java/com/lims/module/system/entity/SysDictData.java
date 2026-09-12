package com.lims.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_dict_data")
public class SysDictData {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String dictCode;
    private String itemLabel;
    private String itemValue;
    private Integer sortNo;
    private String cssClass;
    private Integer status;
}
