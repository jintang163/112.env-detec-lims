package com.lims.module.sampling.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.module.sampling.dto.AssignDTO;
import com.lims.module.sampling.dto.TaskDetailVO;
import com.lims.module.sampling.dto.TaskQueryDTO;
import com.lims.module.sampling.entity.SamplingTask;
import com.lims.module.sampling.service.SamplingPlanService;
import com.lims.module.sampling.service.SamplingTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 采样任务分配(PC)。
 */
@RestController
@RequestMapping("/sampling/tasks")
@RequiredArgsConstructor
public class SamplingTaskController {

    private final SamplingTaskService taskService;
    private final SamplingPlanService planService;

    @GetMapping
    public Result<PageResult<SamplingTask>> page(TaskQueryDTO query) {
        Page<SamplingTask> page = taskService.page(query);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<TaskDetailVO> detail(@PathVariable Long id) {
        return Result.ok(taskService.detail(id));
    }

    /** 派工: 为已下发计划指派采样员 */
    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('PERM_sampling:task:assign')")
    public Result<Long> assign(@Valid @RequestBody AssignDTO dto) {
        return Result.ok(planService.assign(dto.getPlanId(), dto.getAssigneeId(), dto.getRemark()));
    }
}
