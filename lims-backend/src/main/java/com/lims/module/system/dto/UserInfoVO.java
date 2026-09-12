package com.lims.module.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private Long deptId;
    private List<String> roles;
    private List<String> permissions;
    private List<MenuNode> menus;
}
