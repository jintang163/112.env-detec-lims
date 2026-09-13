package com.lims.module.sampling.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.common.util.CodeGenerator;
import com.lims.module.sampling.dto.CheckoutDTO;
import com.lims.module.sampling.dto.EquipmentQueryDTO;
import com.lims.module.sampling.dto.EquipmentReturnDTO;
import com.lims.module.sampling.dto.EquipmentSaveDTO;
import com.lims.module.sampling.entity.Equipment;
import com.lims.module.sampling.entity.EquipmentCheckout;
import com.lims.module.sampling.mapper.EquipmentCheckoutMapper;
import com.lims.module.sampling.mapper.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采样设备/容器台账与领用归还, qty_available 实时联动。
 */
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final EquipmentCheckoutMapper checkoutMapper;
    private final CodeGenerator codeGenerator;

    public Page<Equipment> page(EquipmentQueryDTO q) {
        return equipmentMapper.selectPage(new Page<>(q.getCurrent(), q.getSize()),
                Wrappers.<Equipment>lambdaQuery()
                        .and(StringUtils.hasText(q.getKeyword()),
                                w -> w.like(Equipment::getName, q.getKeyword())
                                        .or().like(Equipment::getCode, q.getKeyword()))
                        .eq(StringUtils.hasText(q.getCategory()), Equipment::getCategory, q.getCategory())
                        .eq(StringUtils.hasText(q.getStatus()), Equipment::getStatus, q.getStatus())
                        .orderByDesc(Equipment::getId));
    }

    /** 下拉选项: 仅正常且有可用量 */
    public List<Equipment> options(String category) {
        return equipmentMapper.selectList(Wrappers.<Equipment>lambdaQuery()
                .eq(StringUtils.hasText(category), Equipment::getCategory, category)
                .eq(Equipment::getStatus, "NORMAL")
                .gt(Equipment::getQtyAvailable, 0)
                .orderByAsc(Equipment::getCode));
    }

    public Page<EquipmentCheckout> checkoutPage(EquipmentQueryDTO q) {
        return checkoutMapper.selectPage(new Page<>(q.getCurrent(), q.getSize()),
                Wrappers.<EquipmentCheckout>lambdaQuery()
                        .eq(StringUtils.hasText(q.getStatus()), EquipmentCheckout::getStatus, q.getStatus())
                        .orderByDesc(EquipmentCheckout::getId));
    }

    @Transactional
    public Long save(EquipmentSaveDTO dto) {
        Equipment e;
        if (dto.getId() == null) {
            e = new Equipment();
            org.springframework.beans.BeanUtils.copyProperties(dto, e, "id", "qtyAvailable");
            e.setCode(codeGenerator.next("SB", false));
            if (e.getQtyTotal() == null || e.getQtyTotal() < 0) {
                e.setQtyTotal(1);
            }
            e.setQtyAvailable(e.getQtyTotal());
            equipmentMapper.insert(e);
        } else {
            e = equipmentMapper.selectById(dto.getId());
            if (e == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            Integer oldTotal = e.getQtyTotal();
            org.springframework.beans.BeanUtils.copyProperties(dto, e,
                    "id", "code", "qtyAvailable", "qtyTotal");
            if (dto.getQtyTotal() != null) {
                // 总量调整时, 可用量等额增减
                int delta = dto.getQtyTotal() - oldTotal;
                e.setQtyTotal(dto.getQtyTotal());
                e.setQtyAvailable(Math.max(0, e.getQtyAvailable() + delta));
            }
            equipmentMapper.updateById(e);
        }
        return e.getId();
    }

    @Transactional
    public Long checkout(CheckoutDTO dto) {
        Equipment e = equipmentMapper.selectById(dto.getEquipmentId());
        if (e == null) {
            throw new BusinessException("设备不存在");
        }
        if (!"NORMAL".equals(e.getStatus())) {
            throw new BusinessException("设备维修/报废中, 不可领用");
        }
        int qty = dto.getQty() == null ? 1 : dto.getQty();
        if (e.getQtyAvailable() < qty) {
            throw new BusinessException("可用库存不足: 仅剩 " + e.getQtyAvailable() + " " + e.getUnit());
        }
        e.setQtyAvailable(e.getQtyAvailable() - qty);
        equipmentMapper.updateById(e);

        SecurityUtils.LoginUser me = SecurityUtils.current();
        EquipmentCheckout c = new EquipmentCheckout();
        c.setEquipmentId(e.getId());
        c.setEquipmentName(e.getName());
        c.setPlanId(dto.getPlanId());
        c.setTaskId(dto.getTaskId());
        c.setQty(qty);
        c.setCheckoutById(me.getUserId());
        c.setCheckoutByName(me.getRealName());
        c.setCheckoutTime(LocalDateTime.now());
        c.setExpectedReturnTime(dto.getExpectedReturnTime());
        c.setCheckoutRemark(dto.getCheckoutRemark());
        c.setStatus("BORROWED");
        c.setCreateBy(me.getUsername());
        checkoutMapper.insert(c);
        return c.getId();
    }

    @Transactional
    public void doReturn(Long checkoutId, EquipmentReturnDTO dto) {
        EquipmentCheckout c = checkoutMapper.selectById(checkoutId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!"BORROWED".equals(c.getStatus())) {
            throw new BusinessException("该记录已归还");
        }
        SecurityUtils.LoginUser me = SecurityUtils.current();
        c.setStatus("RETURNED");
        c.setReturnTime(LocalDateTime.now());
        c.setReturnById(me.getUserId());
        c.setReturnByName(me.getRealName());
        c.setCheckResult(dto.getCheckResult());
        c.setReturnRemark(dto.getReturnRemark());
        checkoutMapper.updateById(c);

        // 仅完好的归还回补可用量; 损坏/缺失不再进入可用库存
        if ("OK".equals(dto.getCheckResult())) {
            Equipment e = equipmentMapper.selectById(c.getEquipmentId());
            if (e != null) {
                e.setQtyAvailable(Math.min(e.getQtyTotal(), e.getQtyAvailable() + c.getQty()));
                equipmentMapper.updateById(e);
            }
        }
    }
}
