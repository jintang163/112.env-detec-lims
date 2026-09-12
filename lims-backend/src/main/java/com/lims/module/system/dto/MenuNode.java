package com.lims.module.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuNode {
    private Long id;
    private Long parentId;
    private String permCode;
    private String permName;
    private String path;
    private String icon;
    private Integer sortNo;
    private List<MenuNode> children = new ArrayList<>();
}
