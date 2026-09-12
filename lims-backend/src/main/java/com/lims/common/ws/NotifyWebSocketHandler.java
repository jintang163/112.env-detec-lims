package com.lims.common.ws;

import io.jsonwebtoken.Claims;
import com.lims.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 站内通知 WebSocket, 连接路径: /api/ws/notify?token=xxx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider tokenProvider;

    /** userId -> 在线会话(同一用户多端登录) */
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUserId(session);
        if (userId == null) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException ignored) {
            }
            return;
        }
        session.getAttributes().put("userId", userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WebSocket 已连接 userId={}, 当前在线={}", userId, userSessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object uid = session.getAttributes().get("userId");
        if (uid instanceof Long) {
            Set<WebSocketSession> set = userSessions.get(uid);
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) {
                    userSessions.remove(uid);
                }
            }
        }
    }

    public void sendToUser(Long userId, WsMessage msg) {
        Set<WebSocketSession> set = userSessions.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        String json = cn.hutool.json.JSONUtil.toJsonStr(msg);
        for (WebSocketSession s : set) {
            try {
                if (s.isOpen()) {
                    synchronized (s) {
                        s.sendMessage(new TextMessage(json));
                    }
                }
            } catch (IOException e) {
                log.warn("WebSocket 推送失败 userId={}: {}", userId, e.getMessage());
            }
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        String query = session.getUri() == null ? null : session.getUri().getQuery();
        if (query == null) {
            return null;
        }
        for (String kv : query.split("&")) {
            String[] arr = kv.split("=", 2);
            if (arr.length == 2 && "token".equals(arr[0])) {
                try {
                    Claims claims = tokenProvider.parse(java.net.URLDecoder.decode(arr[1], "UTF-8"));
                    Object uid = claims.get("uid");
                    if (uid instanceof Number) {
                        return ((Number) uid).longValue();
                    }
                    return Long.valueOf(String.valueOf(uid));
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}
