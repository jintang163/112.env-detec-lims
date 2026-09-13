package com.lims.module.sampling.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.module.sampling.dto.HandoverDetailVO;
import com.lims.module.sampling.dto.HandoverQueryDTO;
import com.lims.module.sampling.entity.SampleHandover;
import com.lims.module.sampling.service.HandoverService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 样品交接(PC): 样品管理员核对、接收/拒收。
 */
@RestController
@RequestMapping("/sampling/handovers")
@RequiredArgsConstructor
public class HandoverController {

    private final HandoverService handoverService;

    @GetMapping
    public Result<PageResult<SampleHandover>> page(HandoverQueryDTO query) {
        Page<SampleHandover> page = handoverService.page(query);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<HandoverDetailVO> detail(@PathVariable Long id) {
        return Result.ok(handoverService.detail(id));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('PERM_sampling:handover:confirm')")
    public Result<Void> confirm(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        handoverService.confirm(id, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PERM_sampling:handover:confirm')")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        handoverService.reject(id, body == null ? null : body.get("reason"));
        return Result.ok();
    }
}
