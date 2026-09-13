package com.lims.module.sampling.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.module.sampling.dto.PlanDetailVO;
import com.lims.module.sampling.dto.SamplingPlanQueryDTO;
import com.lims.module.sampling.dto.SamplingPlanSaveDTO;
import com.lims.module.sampling.entity.SamplingPlan;
import com.lims.module.sampling.entity.SamplingTask;
import com.lims.module.sampling.service.SamplingPlanService;
import com.lims.module.sampling.service.SamplingTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 采样计划(PC): 制定/下发/取消/派工。
 */
@RestController
@RequestMapping("/sampling/plans")
@RequiredArgsConstructor
public class SamplingPlanController {

    private final SamplingPlanService planService;
    private final SamplingTaskService taskService;

    @GetMapping
    public Result<PageResult<SamplingPlan>> page(SamplingPlanQueryDTO query) {
        Page<SamplingPlan> page = planService.page(query);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<PlanDetailVO> detail(@PathVariable Long id) {
        return Result.ok(planService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_sampling:plan:save')")
    public Result<Long> save(@Valid @RequestBody SamplingPlanSaveDTO dto) {
        return Result.ok(planService.save(dto));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('PERM_sampling:plan:issue')")
    public Result<Void> issue(@PathVariable Long id) {
        planService.issue(id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_sampling:plan:issue')")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        planService.cancel(id);
        return Result.ok();
    }

    /** 计划下的任务列表 */
    @GetMapping("/{id}/tasks")
    public Result<List<SamplingTask>> tasks(@PathVariable Long id) {
        return Result.ok(planService.detail(id).getTasks());
    }
}
