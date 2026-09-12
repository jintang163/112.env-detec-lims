package com.lims.module.customer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.module.customer.dto.CustomerQueryDTO;
import com.lims.module.customer.dto.CustomerSaveDTO;
import com.lims.module.customer.dto.CustomerVO;
import com.lims.module.customer.dto.CreditAdjustDTO;
import com.lims.module.customer.entity.Customer;
import com.lims.module.customer.entity.CustomerCreditLog;
import com.lims.module.customer.entity.CustomerFollow;
import com.lims.module.customer.entity.CustomerPoolLog;
import com.lims.module.customer.entity.CustomerQualification;
import com.lims.module.customer.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerCreditService creditService;
    private final CustomerQualificationService qualificationService;
    private final CustomerFollowService followService;

    @GetMapping
    public Result<PageResult<CustomerVO>> page(CustomerQueryDTO query) {
        Page<CustomerVO> page = customerService.page(query);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<Customer> detail(@PathVariable Long id) {
        return Result.ok(customerService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_customer:add')")
    public Result<Long> save(@Valid @RequestBody CustomerSaveDTO dto) {
        return Result.ok(customerService.save(dto));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('PERM_customer:pool')")
    public Result<Void> claim(@PathVariable Long id) {
        customerService.claim(id);
        return Result.ok();
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('PERM_customer:pool')")
    public Result<Void> release(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        customerService.release(id, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public Result<Void> transfer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        customerService.transfer(id, Long.valueOf(body.get("userId").toString()),
                body.get("remark") == null ? null : body.get("remark").toString());
        return Result.ok();
    }

    @GetMapping("/{id}/pool-logs")
    public Result<List<CustomerPoolLog>> poolLogs(@PathVariable Long id) {
        return Result.ok(customerService.poolLogs(id));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public Result<Void> status(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        customerService.toggleStatus(id, body.get("status"));
        return Result.ok();
    }

    // ---------------- 信用管理 ----------------

    @PostMapping("/credit/adjust")
    public Result<Void> adjustCredit(@Valid @RequestBody CreditAdjustDTO dto) {
        creditService.adjust(dto);
        return Result.ok();
    }

    @GetMapping("/{id}/credit-logs")
    public Result<List<CustomerCreditLog>> creditLogs(@PathVariable Long id) {
        return Result.ok(creditService.logs(id));
    }

    // ---------------- 资质文件 ----------------

    @GetMapping("/{id}/qualifications")
    public Result<List<CustomerQualification>> qualifications(@PathVariable Long id) {
        return Result.ok(qualificationService.list(id));
    }

    @PostMapping("/qualifications")
    @PreAuthorize("hasAuthority('PERM_customer:edit')")
    public Result<Long> saveQualification(@RequestBody CustomerQualification q) {
        return Result.ok(qualificationService.save(q));
    }

    @DeleteMapping("/qualifications/{id}")
    public Result<Void> deleteQualification(@PathVariable Long id) {
        qualificationService.delete(id);
        return Result.ok();
    }

    // ---------------- 跟进记录 ----------------

    @PostMapping("/follows")
    public Result<Void> addFollow(@RequestBody CustomerFollow follow) {
        followService.add(follow);
        return Result.ok();
    }

    @GetMapping("/{id}/follows")
    public Result<List<CustomerFollow>> follows(@PathVariable Long id) {
        return Result.ok(followService.list(id));
    }
}
