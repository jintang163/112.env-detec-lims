package com.lims.common.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器。
 * 用户权限在登录时写入 Redis(lims:auth:{userId}), 请求时读取, 避免每请求查库;
 * 修改角色/权限后删除该 key 即可强制刷新。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_CACHE_PREFIX = "lims:auth:";

    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Claims claims = tokenProvider.parse(token);
                Long userId = claims.get("uid", Long.class);
                String username = (String) claims.get("username");

                String cached = redisTemplate.opsForValue().get(AUTH_CACHE_PREFIX + userId);
                if (cached != null) {
                    AuthCache ac = AuthCache.parse(cached);
                    SecurityUtils.LoginUser loginUser = new SecurityUtils.LoginUser();
                    loginUser.setUserId(userId);
                    loginUser.setUsername(username);
                    loginUser.setRealName(ac.getRealName());
                    loginUser.setDeptId(ac.getDeptId());
                    loginUser.setRoles(ac.getRoles());
                    loginUser.setPermissions(ac.getPermissions());

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    ac.getRoles().forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
                    ac.getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_" + p)));

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.debug("JWT 解析失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        // WebSocket 握手时 token 走 query 参数
        String param = request.getParameter("token");
        return param != null && !param.isEmpty() ? param : null;
    }

    public static String authCacheKey(Long userId) {
        return AUTH_CACHE_PREFIX + userId;
    }
}
