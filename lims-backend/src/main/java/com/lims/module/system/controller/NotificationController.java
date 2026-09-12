package com.lims.module.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.common.security.SecurityUtils;
import com.lims.module.system.entity.SysNotification;
import com.lims.module.system.mapper.SysNotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SysNotificationMapper notificationMapper;

    @GetMapping
    public Result<PageResult<SysNotification>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer isRead) {
        Page<SysNotification> page = notificationMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysNotification>lambdaQuery()
                        .eq(SysNotification::getUserId, SecurityUtils.currentUserId())
                        .eq(isRead != null, SysNotification::getIsRead, isRead)
                        .orderByDesc(SysNotification::getId));
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationMapper.selectCount(Wrappers.<SysNotification>lambdaQuery()
                .eq(SysNotification::getUserId, SecurityUtils.currentUserId())
                .eq(SysNotification::getIsRead, 0)));
    }

    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        SysNotification n = notificationMapper.selectById(id);
        if (n != null && n.getUserId().equals(SecurityUtils.currentUserId())) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
        return Result.ok();
    }

    @PostMapping("/read-all")
    public Result<Void> readAll() {
        SysNotification n = new SysNotification();
        n.setIsRead(1);
        notificationMapper.update(n, Wrappers.<SysNotification>lambdaUpdate()
                .eq(SysNotification::getUserId, SecurityUtils.currentUserId())
                .eq(SysNotification::getIsRead, 0));
        return Result.ok();
    }
}
