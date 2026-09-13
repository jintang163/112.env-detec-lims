package com.lims.module.sampling.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.common.security.SecurityUtils;
import com.lims.module.sampling.dto.HandoverCreateDTO;
import com.lims.module.sampling.dto.SampleSubmitDTO;
import com.lims.module.sampling.dto.SampleVO;
import com.lims.module.sampling.dto.TaskDetailVO;
import com.lims.module.sampling.dto.TaskQueryDTO;
import com.lims.module.sampling.entity.FieldSample;
import com.lims.module.sampling.entity.SampleHandover;
import com.lims.module.sampling.entity.SamplingTask;
import com.lims.module.sampling.service.FieldSampleService;
import com.lims.module.sampling.service.HandoverService;
import com.lims.module.sampling.service.SamplingTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 移动端采样接口(uni-app): 我的任务/离线整包下载/样品幂等提交/发起交接/扫码核验。
 * 与 MobileController 一致, 登录态即可访问, 不做方法级权限限制。
 */
@RestController
@RequestMapping("/mobile/sampling")
@RequiredArgsConstructor
public class MobileSamplingController {

    private final SamplingTaskService taskService;
    private final FieldSampleService fieldSampleService;
    private final HandoverService handoverService;

    /** 派给我的采样任务 */
    @GetMapping("/my-tasks")
    public Result<PageResult<SamplingTask>> myTasks(TaskQueryDTO query) {
        Page<SamplingTask> page = taskService.myTasks(query, SecurityUtils.currentUserId());
        return Result.ok(PageResult.of(page));
    }

    /** 任务详情整包(任务+计划+点位+检测项+设备+样品照片), 供离线下载 */
    @GetMapping("/tasks/{id}")
    public Result<TaskDetailVO> taskDetail(@PathVariable Long id) {
        return Result.ok(taskService.detail(id));
    }

    /** 记录移动端下载时间 */
    @PostMapping("/tasks/{id}/download")
    public Result<Void> downloaded(@PathVariable Long id) {
        fieldSampleService.markDownloaded(id);
        return Result.ok();
    }

    /** 提交现场样品(离线重传按 clientUuid 幂等) */
    @PostMapping("/samples")
    public Result<FieldSample> submitSample(@Valid @RequestBody SampleSubmitDTO dto) {
        return Result.ok(fieldSampleService.submit(dto));
    }

    /** 采样员标记采样完成(待交接) */
    @PostMapping("/tasks/{id}/submit")
    public Result<Void> submitTask(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        fieldSampleService.markTaskSubmitted(id, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    /** 发起样品交接 */
    @PostMapping("/handovers")
    public Result<Long> createHandover(@Valid @RequestBody HandoverCreateDTO dto) {
        return Result.ok(handoverService.create(dto));
    }

    /** 我发起的交接 */
    @GetMapping("/handovers/my")
    public Result<List<SampleHandover>> myHandovers() {
        return Result.ok(handoverService.myHandovers(SecurityUtils.currentUserId()));
    }

    /** 扫码核验样品 */
    @GetMapping("/samples/{code}")
    public Result<SampleVO> sampleByCode(@PathVariable String code) {
        return Result.ok(fieldSampleService.getVOByCode(code));
    }
}
