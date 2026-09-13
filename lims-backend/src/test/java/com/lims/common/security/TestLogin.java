package com.lims.common.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Arrays;
import java.util.Collections;

/**
 * 测试辅助: 向 SecurityContext 写入登录用户, 模拟已认证请求。
 */
public final class TestLogin {

    private TestLogin() {
    }

    public static void loginAs(Long userId, String... roles) {
        SecurityUtils.LoginUser u = new SecurityUtils.LoginUser();
        u.setUserId(userId);
        u.setUsername("user" + userId);
        u.setRealName("用户" + userId);
        u.setRoles(Arrays.asList(roles));
        u.setPermissions(Collections.emptyList());
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList())));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
