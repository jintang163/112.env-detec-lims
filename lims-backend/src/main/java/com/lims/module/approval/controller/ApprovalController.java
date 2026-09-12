package com.lims.module.approval.controller;

import com.lims.common.core.Result;
import com.lims.common.security.SecurityUtils;
import com.lims.module.approval.entity.ApprovalRecord;
import com.lims.module.approval.entity.ApprovalTask;
import com.lims.module.approval.service.ApprovalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/todo")
    public Result<List<java.util.Map<String, Object>>> todo() {
        SecurityUtils.LoginUser u = SecurityUtils.current();
        return Result.ok(approvalService.myTodo(u.getUserId(), u.getRoles()));
    }

    @GetMapping("/todo-count")
    public Result<Long> todoCount() {
        return Result.ok(approvalService.myTodoCount(SecurityUtils.current().getRoles()));
    }

    @GetMapping("/timeline/{instanceId}")
    public Result<List<ApprovalRecord>> timeline(@PathVariable Long instanceId) {
        return Result.ok(approvalService.timeline(instanceId));
    }

    @PostMapping("/act")
    public Result<Void> act(@RequestBody ActDTO dto) {
        SecurityUtils.LoginUser u = SecurityUtils.current();
        approvalService.act(dto.getTaskId(), dto.getApprove(), dto.getComment(),
                u.getUserId(), u.getRealName(), u.getRoles());
        return Result.ok();
    }

    @Data
    public static class ActDTO {
        @NotNull(message = "缺少审批任务")
        private Long taskId;
        @NotNull
        private Boolean approve;
        private String comment;
    }
}
