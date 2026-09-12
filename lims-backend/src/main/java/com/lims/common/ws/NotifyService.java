package com.lims.common.ws;

import com.lims.module.system.entity.SysNotification;
import com.lims.module.system.mapper.SysNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 通知服务: 落库 + WebSocket 实时推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final NotifyWebSocketHandler wsHandler;
    private final SysNotificationMapper notificationMapper;

    /**
     * 给指定用户发送一条待办/通知
     */
    @Async
    public void push(Long userId, String title, String content, String bizType, Long bizId) {
        SysNotification n = new SysNotification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setBizType(bizType);
        n.setBizId(bizId);
        n.setIsRead(0);
        notificationMapper.insert(n);

        WsMessage msg = WsMessage.notification(title, content, bizType, bizId);
        wsHandler.sendToUser(userId, msg);
    }

    @Async
    public void pushTodo(Long userId, String title, String content, String bizType, Long bizId) {
        push(userId, title, content, bizType, bizId);
    }
}
