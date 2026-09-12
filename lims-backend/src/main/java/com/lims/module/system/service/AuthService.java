package com.lims.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.AuthCache;
import com.lims.common.security.JwtAuthenticationFilter;
import com.lims.common.security.JwtTokenProvider;
import com.lims.module.system.dto.LoginDTO;
import com.lims.module.system.dto.LoginVO;
import com.lims.module.system.dto.MenuNode;
import com.lims.module.system.dto.UserInfoVO;
import com.lims.module.system.entity.SysPermission;
import com.lims.module.system.entity.SysUser;
import com.lims.module.system.mapper.SysPermissionMapper;
import com.lims.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        List<String> roles = userMapper.selectRoleCodes(user.getId());
        List<String> perms = userMapper.selectPermCodes(user.getId());

        AuthCache cache = new AuthCache();
        cache.setRealName(user.getRealName());
        cache.setDeptId(user.getDeptId());
        cache.setRoles(roles);
        cache.setPermissions(perms);
        // 权限快照有效期略长于 token
        redisTemplate.opsForValue().set(JwtAuthenticationFilter.authCacheKey(user.getId()),
                cache.toJson(), 14, TimeUnit.HOURS);

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        LoginVO vo = new LoginVO();
        vo.setToken(tokenProvider.create(user.getId(), user.getUsername()));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setDeptId(user.getDeptId());
        vo.setRoles(roles);
        vo.setPermissions(perms);
        return vo;
    }

    public void logout(Long userId) {
        redisTemplate.delete(JwtAuthenticationFilter.authCacheKey(userId));
    }

    public UserInfoVO currentUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        List<String> roles = userMapper.selectRoleCodes(userId);
        List<String> perms = userMapper.selectPermCodes(userId);

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(userId);
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setDeptId(user.getDeptId());
        vo.setRoles(roles);
        vo.setPermissions(perms);
        vo.setMenus(buildMenuTree(permissionMapper.selectMenusByRoles(roles)));
        return vo;
    }

    private List<MenuNode> buildMenuTree(List<SysPermission> menus) {
        Map<Long, MenuNode> map = new LinkedHashMap<>();
        menus.forEach(p -> {
            MenuNode n = new MenuNode();
            n.setId(p.getId());
            n.setParentId(p.getParentId());
            n.setPermCode(p.getPermCode());
            n.setPermName(p.getPermName());
            n.setPath(p.getPath());
            n.setIcon(p.getIcon());
            n.setSortNo(p.getSortNo());
            map.put(p.getId(), n);
        });
        List<MenuNode> roots = new ArrayList<>();
        map.values().forEach(n -> {
            MenuNode parent = map.get(n.getParentId());
            if (parent != null) {
                parent.getChildren().add(n);
            } else {
                roots.add(n);
            }
        });
        Comparator<MenuNode> cmp = Comparator.comparingInt(
                n -> n.getSortNo() == null ? 0 : n.getSortNo());
        roots.sort(cmp);
        roots.forEach(r -> r.getChildren().sort(cmp));
        return roots;
    }

    /** 供审批模块使用: 角色下的用户ID */
    public List<Long> userIdsByRole(String roleCode) {
        if ("ROLE_ADMIN".equals(roleCode)) {
            // 总经理节点演示环境直接发给 admin
            return userMapper.selectUserIdsByRole(roleCode).stream()
                    .filter(id -> id == 1L).collect(Collectors.toList());
        }
        return userMapper.selectUserIdsByRole(roleCode);
    }
}
