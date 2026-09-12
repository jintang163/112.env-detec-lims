package com.lims.common.security;

import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return (LoginUser) auth.getPrincipal();
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    public static LoginUser currentOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return (LoginUser) auth.getPrincipal();
        }
        return null;
    }

    public static Long currentUserId() {
        return current().getUserId();
    }

    public static String currentUsername() {
        return current().getUsername();
    }

    public static String currentUsernameOr(String fallback) {
        LoginUser u = currentOrNull();
        return u == null ? fallback : u.getUsername();
    }

    public static boolean hasRole(String role) {
        LoginUser u = currentOrNull();
        return u != null && u.getRoles().contains(role);
    }

    @Data
    public static class LoginUser {
        private Long userId;
        private String username;
        private String realName;
        private Long deptId;
        private java.util.List<String> roles;
        private java.util.List<String> permissions;
    }
}
