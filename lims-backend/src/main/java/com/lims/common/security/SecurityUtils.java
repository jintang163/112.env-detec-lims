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

    /**
     * 校验当前用户为资源归属人(或 ROLE_ADMIN 管理员), 否则抛 403。
     * 用于移动端任务/样品等按人归属的数据隔离。
     */
    public static void checkOwnerOrAdmin(Long ownerId) {
        checkOwnerOrAdmin(ownerId, "无权操作他人的任务数据");
    }

    public static void checkOwnerOrAdmin(Long ownerId, String message) {
        LoginUser u = current();
        if (ownerId == null || (!ownerId.equals(u.getUserId()) && !hasRole("ROLE_ADMIN"))) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), message);
        }
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
