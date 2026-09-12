package com.lims.module.mobile.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.module.customer.dto.CustomerQueryDTO;
import com.lims.module.customer.dto.CustomerVO;
import com.lims.module.customer.service.CustomerService;
import com.lims.module.entrust.dto.EntrustDetailVO;
import com.lims.module.entrust.dto.EntrustQueryDTO;
import com.lims.module.entrust.entity.EntrustSamplingPoint;
import com.lims.module.entrust.service.EntrustOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 移动端(uni-app)接口: 复用业务服务, 供采样员现场作业。
 * 经纬度约定为百度坐标系 BD-09。
 */
@RestController
@RequestMapping("/mobile")
@RequiredArgsConstructor
public class MobileController {

    private final EntrustOrderService orderService;
    private final CustomerService customerService;

    /** 采样任务列表(采样中/已受理待采样, 支持加急筛选) */
    @GetMapping("/sampling-tasks")
    public Result<PageResult<EntrustDetailVO>> tasks(EntrustQueryDTO query,
                                                     @RequestParam(required = false) String urgency) {
        query.setStatus("SAMPLING");
        Page<EntrustDetailVO> page = orderService.page(query);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/entrusts/{id}")
    public Result<EntrustDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    /** 现场新增采样点位(BD-09) */
    @PostMapping("/entrusts/{id}/points")
    public Result<Long> addPoint(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(orderService.addSamplingPoint(id,
                body.get("name") == null ? "采样点" : body.get("name").toString(),
                new BigDecimal(body.get("lng").toString()),
                new BigDecimal(body.get("lat").toString()),
                body.get("addrDesc") == null ? null : body.get("addrDesc").toString()));
    }

    /** 采样完成 -> 进入检测 */
    @PostMapping("/entrusts/{id}/finish-sampling")
    public Result<Void> finishSampling(@PathVariable Long id) {
        orderService.progress(id, "START_TESTING", "移动端采样完成,样品已交接");
        return Result.ok();
    }

    @GetMapping("/customers")
    public Result<PageResult<CustomerVO>> customers(CustomerQueryDTO query) {
        return Result.ok(PageResult.of(customerService.page(query)));
    }
}
