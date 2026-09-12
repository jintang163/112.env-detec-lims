package com.lims.common.security;

import cn.hutool.json.JSONUtil;
import lombok.Data;

import java.util.List;

/**
 * 登录用户在 Redis 中的权限快照
 */
@Data
public class AuthCache {
    private String realName;
    private Long deptId;
    private List<String> roles;
    private List<String> permissions;

    public String toJson() {
        return JSONUtil.toJsonStr(this);
    }

    public static AuthCache parse(String json) {
        return JSONUtil.toBean(json, AuthCache.class);
    }
}
