package com.lims.module.contract.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.module.contract.dto.ContractQueryDTO;
import com.lims.module.contract.dto.ContractSaveDTO;
import com.lims.module.contract.dto.ContractVO;
import com.lims.module.contract.entity.Contract;
import com.lims.module.contract.entity.ContractChange;
import com.lims.module.contract.entity.ContractPayment;
import com.lims.module.contract.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ContractPaymentService paymentService;
    private final ContractChangeService changeService;

    @GetMapping
    public Result<PageResult<ContractVO>> page(ContractQueryDTO query) {
        Page<ContractVO> page = contractService.page(query);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<Contract> detail(@PathVariable Long id) {
        return Result.ok(contractService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_contract:add')")
    public Result<Long> save(@Valid @RequestBody ContractSaveDTO dto) {
        return Result.ok(contractService.save(dto));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('PERM_contract:add')")
    public Result<Void> submit(@PathVariable Long id) {
        contractService.submit(id);
        return Result.ok();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public Result<Void> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        contractService.changeStatus(id, body.get("status"), body.get("remark"));
        return Result.ok();
    }

    @GetMapping("/{id}/performance")
    public Result<Map<String, Object>> performance(@PathVariable Long id) {
        return Result.ok(contractService.performance(id));
    }

    // ---------------- 履约: 收款计划/收款/开票 ----------------

    @GetMapping("/{id}/payments")
    public Result<List<ContractPayment>> payments(@PathVariable Long id,
                                                  @RequestParam(required = false) Integer payType) {
        return Result.ok(paymentService.list(id, payType));
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FINANCE','ROLE_MANAGER')")
    public Result<Long> savePayment(@RequestBody ContractPayment payment) {
        return Result.ok(paymentService.save(payment));
    }

    @DeleteMapping("/payments/{paymentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FINANCE')")
    public Result<Void> deletePayment(@PathVariable Long paymentId) {
        paymentService.delete(paymentId);
        return Result.ok();
    }

    // ---------------- 合同变更 ----------------

    @PostMapping("/changes")
    @PreAuthorize("hasAuthority('PERM_contract:change')")
    public Result<Long> applyChange(@RequestBody ContractChange change) {
        return Result.ok(changeService.apply(change));
    }

    @GetMapping("/{id}/changes")
    public Result<List<ContractChange>> changes(@PathVariable Long id) {
        return Result.ok(changeService.list(id));
    }
}
