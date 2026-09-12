package com.lims.module.entrust.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.module.entrust.dto.EntrustDetailVO;
import com.lims.module.entrust.dto.EntrustQueryDTO;
import com.lims.module.entrust.dto.EntrustSaveDTO;
import com.lims.module.entrust.entity.EntrustAdjustment;
import com.lims.module.entrust.entity.EntrustItem;
import com.lims.module.entrust.entity.EntrustSamplingPoint;
import com.lims.module.entrust.entity.EntrustStatusLog;
import com.lims.module.entrust.entity.EntrustSubcontract;
import com.lims.module.entrust.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/entrusts")
@RequiredArgsConstructor
public class EntrustController {

    private final EntrustOrderService orderService;
    private final EntrustAdjustmentService adjustmentService;
    private final EntrustSubcontractService subcontractService;

    @GetMapping
    public Result<PageResult<EntrustDetailVO>> page(EntrustQueryDTO query) {
        Page<EntrustDetailVO> page = orderService.page(query);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<EntrustDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_entrust:add')")
    public Result<Long> save(@Valid @RequestBody EntrustSaveDTO dto) {
        return Result.ok(orderService.save(dto));
    }

    /** 从已审批报价单一键生成委托单 */
    @PostMapping("/from-quote/{quoteId}")
    @PreAuthorize("hasAuthority('PERM_entrust:add')")
    public Result<Long> fromQuote(@PathVariable Long quoteId, @RequestBody EntrustSaveDTO dto) {
        return Result.ok(orderService.createFromQuote(quoteId, dto));
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasAuthority('PERM_entrust:add')")
    public Result<Void> submitReview(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        orderService.submitReview(id, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/{id}/progress")
    @PreAuthorize("hasAuthority('PERM_entrust:progress')")
    public Result<Void> progress(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.progress(id, body.get("action"), body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.cancel(id, body.get("reason"));
        return Result.ok();
    }

    @PostMapping("/{id}/urgent")
    public Result<Void> urgent(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        orderService.toggleUrgent(id, Boolean.TRUE.equals(body.get("urgent")),
                body.get("reason") == null ? null : body.get("reason").toString());
        return Result.ok();
    }

    @GetMapping("/{id}/timeline")
    public Result<List<EntrustStatusLog>> timeline(@PathVariable Long id) {
        return Result.ok(orderService.timeline(id));
    }

    @GetMapping("/{id}/items")
    public Result<List<EntrustItem>> items(@PathVariable Long id) {
        return Result.ok(orderService.items(id));
    }

    @GetMapping("/{id}/points")
    public Result<List<EntrustSamplingPoint>> points(@PathVariable Long id) {
        return Result.ok(orderService.points(id));
    }

    // ---------------- 调账 ----------------

    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('PERM_entrust:adjust')")
    public Result<Long> applyAdjustment(@RequestBody EntrustAdjustment a) {
        return Result.ok(adjustmentService.apply(a));
    }

    @GetMapping("/{id}/adjustments")
    public Result<List<EntrustAdjustment>> adjustments(@PathVariable Long id) {
        return Result.ok(adjustmentService.list(id));
    }

    // ---------------- 分包 ----------------

    @PostMapping("/subcontracts")
    @PreAuthorize("hasAuthority('PERM_entrust:subcontract')")
    public Result<Long> applySubcontract(@RequestBody EntrustSubcontract s) {
        return Result.ok(subcontractService.apply(s));
    }

    @GetMapping("/{id}/subcontracts")
    public Result<List<EntrustSubcontract>> subcontracts(@PathVariable Long id) {
        return Result.ok(subcontractService.list(id));
    }
}
