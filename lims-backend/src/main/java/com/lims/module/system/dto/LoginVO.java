package com.lims.module.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private Long deptId;
    private List<String> roles;
    private List<String> permissions;
}
