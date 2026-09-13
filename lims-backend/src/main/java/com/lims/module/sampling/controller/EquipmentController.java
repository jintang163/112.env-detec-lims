package com.lims.module.sampling.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.module.sampling.dto.CheckoutDTO;
import com.lims.module.sampling.dto.EquipmentQueryDTO;
import com.lims.module.sampling.dto.EquipmentReturnDTO;
import com.lims.module.sampling.dto.EquipmentSaveDTO;
import com.lims.module.sampling.entity.Equipment;
import com.lims.module.sampling.entity.EquipmentCheckout;
import com.lims.module.sampling.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 采样设备/容器台账与领用归还(PC)。
 */
@RestController
@RequestMapping("/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    public Result<PageResult<Equipment>> page(EquipmentQueryDTO query) {
        Page<Equipment> page = equipmentService.page(query);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/options")
    public Result<List<Equipment>> options(@RequestParam(required = false) String category) {
        return Result.ok(equipmentService.options(category));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_equipment:save')")
    public Result<Long> save(@Valid @RequestBody EquipmentSaveDTO dto) {
        return Result.ok(equipmentService.save(dto));
    }

    @GetMapping("/checkouts")
    public Result<PageResult<EquipmentCheckout>> checkoutPage(EquipmentQueryDTO query) {
        Page<EquipmentCheckout> page = equipmentService.checkoutPage(query);
        return Result.ok(PageResult.of(page));
    }

    @PostMapping("/checkouts")
    @PreAuthorize("hasAuthority('PERM_equipment:checkout')")
    public Result<Long> checkout(@Valid @RequestBody CheckoutDTO dto) {
        return Result.ok(equipmentService.checkout(dto));
    }

    @PostMapping("/checkouts/{id}/return")
    @PreAuthorize("hasAuthority('PERM_equipment:checkout')")
    public Result<Void> doReturn(@PathVariable Long id, @Valid @RequestBody EquipmentReturnDTO dto) {
        equipmentService.doReturn(id, dto);
        return Result.ok();
    }
}
