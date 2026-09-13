package com.lims.module.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.module.system.entity.SysOperationLog;
import com.lims.module.system.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 操作日志查询(仅管理员)。写操作由 OperationLogAspect 自动记录, 这里只读。 */
@RestController
@RequestMapping("/system/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final SysOperationLogMapper operationLogMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_system:operation-log')")
    public Result<PageResult<SysOperationLog>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String keyword) {
        Page<SysOperationLog> page = operationLogMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysOperationLog>lambdaQuery()
                        .eq(StringUtils.hasText(module), SysOperationLog::getModule, module)
                        .like(StringUtils.hasText(username), SysOperationLog::getUsername, username)
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(SysOperationLog::getAction, keyword)
                                .or().like(SysOperationLog::getMethod, keyword))
                        .orderByDesc(SysOperationLog::getId));
        return Result.ok(PageResult.of(page));
    }
}
