package com.lims.module.sampling.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.common.util.CodeGenerator;
import com.lims.common.ws.NotifyService;
import com.lims.module.customer.entity.Customer;
import com.lims.module.customer.mapper.CustomerMapper;
import com.lims.module.entrust.entity.EntrustItem;
import com.lims.module.entrust.entity.EntrustOrder;
import com.lims.module.entrust.entity.EntrustSamplingPoint;
import com.lims.module.entrust.mapper.EntrustItemMapper;
import com.lims.module.entrust.mapper.EntrustOrderMapper;
import com.lims.module.entrust.mapper.EntrustSamplingPointMapper;
import com.lims.module.sampling.dto.PlanDetailVO;
import com.lims.module.sampling.dto.SamplingPlanQueryDTO;
import com.lims.module.sampling.dto.SamplingPlanSaveDTO;
import com.lims.module.sampling.entity.*;
import com.lims.module.sampling.mapper.*;
import com.lims.module.system.entity.SysUser;
import com.lims.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 采样计划: 基于已受理/采样中的委托单制定, 下发后可派工。
 */
@Service
@RequiredArgsConstructor
public class SamplingPlanService {

    private final SamplingPlanMapper planMapper;
    private final SamplingPlanPointMapper planPointMapper;
    private final SamplingPlanItemMapper planItemMapper;
    private final SamplingPlanEquipmentMapper planEquipmentMapper;
    private final SamplingTaskMapper taskMapper;
    private final EntrustOrderMapper orderMapper;
    private final EntrustItemMapper entrustItemMapper;
    private final EntrustSamplingPointMapper entrustPointMapper;
    private final CustomerMapper customerMapper;
    private final SysUserMapper sysUserMapper;
    private final CodeGenerator codeGenerator;
    private final NotifyService notifyService;

    @Transactional
    public Long save(SamplingPlanSaveDTO dto) {
        EntrustOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException("委托单不存在");
        }
        if (!"ACCEPTED".equals(order.getStatus()) && !"SAMPLING".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "仅已受理/采样中的委托单可制定采样计划");
        }

        SamplingPlan plan;
        if (dto.getId() == null) {
            plan = new SamplingPlan();
            plan.setCode(codeGenerator.next("CYJH", false));
            plan.setStatus(SamplingStatus.PLAN_DRAFT);
            plan.setOrderId(order.getId());
            snapshotFromOrder(plan, order, dto);
            planMapper.insert(plan);
            replaceChildren(plan.getId(), dto, order, true);
        } else {
            plan = planMapper.selectById(dto.getId());
            if (plan == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            if (!SamplingStatus.PLAN_DRAFT.equals(plan.getStatus())) {
                throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "仅草稿状态计划可修改");
            }
            plan.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : plan.getTitle());
            plan.setPlanDate(dto.getPlanDate());
            plan.setStartTime(dto.getStartTime());
            plan.setEndTime(dto.getEndTime());
            plan.setWeather(dto.getWeather());
            plan.setRemark(dto.getRemark());
            planMapper.updateById(plan);
            replaceChildren(plan.getId(), dto, order, false);
        }
        return plan.getId();
    }

    private void snapshotFromOrder(SamplingPlan plan, EntrustOrder order, SamplingPlanSaveDTO dto) {
        plan.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : order.getTitle());
        plan.setPlanDate(dto.getPlanDate());
        plan.setStartTime(dto.getStartTime() != null ? dto.getStartTime() : order.getPlannedSamplingTime());
        plan.setEndTime(dto.getEndTime());
        plan.setWeather(dto.getWeather());
        plan.setRemark(dto.getRemark());
        plan.setCustomerId(order.getCustomerId());
        Customer c = customerMapper.selectById(order.getCustomerId());
        plan.setCustomerName(c == null ? null : c.getName());
        plan.setContactPerson(order.getContactPerson());
        plan.setContactPhone(order.getContactPhone());
        plan.setAddress(order.getSamplingAddress());
        plan.setLng(order.getLng());
        plan.setLat(order.getLat());
    }

    /** 覆盖式重建三张子表; autoCopy=true 且未传明细时从委托单现有点位/检测项带入 */
    private void replaceChildren(Long planId, SamplingPlanSaveDTO dto, EntrustOrder order, boolean autoCopy) {
        planPointMapper.delete(Wrappers.<SamplingPlanPoint>lambdaQuery().eq(SamplingPlanPoint::getPlanId, planId));
        planItemMapper.delete(Wrappers.<SamplingPlanItem>lambdaQuery().eq(SamplingPlanItem::getPlanId, planId));
        planEquipmentMapper.delete(Wrappers.<SamplingPlanEquipment>lambdaQuery().eq(SamplingPlanEquipment::getPlanId, planId));

        boolean pointsGiven = dto.getPoints() != null && !dto.getPoints().isEmpty();
        if (pointsGiven) {
            int i = 1;
            for (SamplingPlanPoint p : dto.getPoints()) {
                p.setId(null);
                p.setPlanId(planId);
                p.setSortNo(p.getSortNo() == null ? i : p.getSortNo());
                planPointMapper.insert(p);
                i++;
            }
        } else if (autoCopy) {
            List<EntrustSamplingPoint> ops = entrustPointMapper.selectList(
                    Wrappers.<EntrustSamplingPoint>lambdaQuery()
                            .eq(EntrustSamplingPoint::getOrderId, order.getId())
                            .orderByAsc(EntrustSamplingPoint::getSortNo));
            int i = 1;
            for (EntrustSamplingPoint sp : ops) {
                SamplingPlanPoint p = new SamplingPlanPoint();
                p.setPlanId(planId);
                p.setPointId(sp.getId());
                p.setName(sp.getName());
                p.setLng(sp.getLng());
                p.setLat(sp.getLat());
                p.setAddrDesc(sp.getAddrDesc());
                p.setSortNo(i++);
                planPointMapper.insert(p);
            }
        }

        boolean itemsGiven = dto.getItems() != null && !dto.getItems().isEmpty();
        if (itemsGiven) {
            int i = 1;
            for (SamplingPlanItem it : dto.getItems()) {
                it.setId(null);
                it.setPlanId(planId);
                it.setSortNo(it.getSortNo() == null ? i : it.getSortNo());
                it.setQcRequired(it.getQcRequired() == null ? 0 : it.getQcRequired());
                planItemMapper.insert(it);
                i++;
            }
        } else if (autoCopy) {
            List<EntrustItem> ois = entrustItemMapper.selectList(
                    Wrappers.<EntrustItem>lambdaQuery()
                            .eq(EntrustItem::getOrderId, order.getId())
                            .orderByAsc(EntrustItem::getSortNo));
            int i = 1;
            for (EntrustItem ei : ois) {
                SamplingPlanItem it = new SamplingPlanItem();
                it.setPlanId(planId);
                it.setOrderItemId(ei.getId());
                it.setItemName(ei.getItemName());
                it.setSampleName(ei.getSampleName());
                it.setSampleQty(ei.getSampleQty());
                it.setQcRequired(0);
                it.setSortNo(i++);
                planItemMapper.insert(it);
            }
        }

        if (dto.getEquipments() != null) {
            for (SamplingPlanEquipment pe : dto.getEquipments()) {
                pe.setId(null);
                pe.setPlanId(planId);
                pe.setQty(pe.getQty() == null || pe.getQty() < 1 ? 1 : pe.getQty());
                planEquipmentMapper.insert(pe);
            }
        }
    }

    @Transactional
    public void issue(Long id) {
        SamplingPlan plan = mustGet(id);
        if (!SamplingStatus.PLAN_DRAFT.equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "仅草稿计划可下发");
        }
        plan.setStatus(SamplingStatus.PLAN_ISSUED);
        planMapper.updateById(plan);
    }

    @Transactional
    public void cancel(Long id) {
        SamplingPlan plan = mustGet(id);
        if (SamplingStatus.PLAN_CANCELLED.equals(plan.getStatus())) {
            return;
        }
        Long active = taskMapper.selectCount(Wrappers.<SamplingTask>lambdaQuery()
                .eq(SamplingTask::getPlanId, id)
                .in(SamplingTask::getStatus, SamplingStatus.TASK_ASSIGNED, SamplingStatus.TASK_SUBMITTED));
        if (active != null && active > 0) {
            throw new BusinessException("存在进行中的采样任务, 不可取消");
        }
        plan.setStatus(SamplingStatus.PLAN_CANCELLED);
        planMapper.updateById(plan);
    }

    /** 派工: 为已下发计划创建采样任务并通知采样员 */
    @Transactional
    public Long assign(Long planId, Long assigneeId, String remark) {
        SamplingPlan plan = mustGet(planId);
        if (!SamplingStatus.PLAN_ISSUED.equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "仅已下发计划可派工");
        }
        SysUser sampler = sysUserMapper.selectById(assigneeId);
        if (sampler == null) {
            throw new BusinessException("采样员不存在");
        }
        Long active = taskMapper.selectCount(Wrappers.<SamplingTask>lambdaQuery()
                .eq(SamplingTask::getPlanId, planId)
                .in(SamplingTask::getStatus, SamplingStatus.TASK_ASSIGNED, SamplingStatus.TASK_SUBMITTED));
        if (active != null && active > 0) {
            throw new BusinessException("该计划已存在进行中的采样任务");
        }
        SamplingTask task = new SamplingTask();
        task.setCode(codeGenerator.next("CYRW", false));
        task.setPlanId(planId);
        task.setOrderId(plan.getOrderId());
        task.setAssigneeId(assigneeId);
        task.setAssigneeName(sampler.getRealName());
        task.setAssignedBy(SecurityUtils.currentUsernameOr("system"));
        task.setStatus(SamplingStatus.TASK_ASSIGNED);
        task.setRemark(remark);
        taskMapper.insert(task);

        notifyService.pushTodo(assigneeId, "新采样任务: " + task.getCode(),
                plan.getTitle() + ", 采样日期 " + plan.getPlanDate(), "SAMPLING_TASK", task.getId());
        return task.getId();
    }

    public Page<SamplingPlan> page(SamplingPlanQueryDTO q) {
        Page<SamplingPlan> page = planMapper.selectPage(new Page<>(q.getCurrent(), q.getSize()),
                Wrappers.<SamplingPlan>lambdaQuery()
                        .and(StringUtils.hasText(q.getKeyword()),
                                w -> w.like(SamplingPlan::getTitle, q.getKeyword())
                                        .or().like(SamplingPlan::getCode, q.getKeyword()))
                        .eq(q.getOrderId() != null, SamplingPlan::getOrderId, q.getOrderId())
                        .eq(StringUtils.hasText(q.getStatus()), SamplingPlan::getStatus, q.getStatus())
                        .ge(q.getPlanDateStart() != null, SamplingPlan::getPlanDate, q.getPlanDateStart())
                        .le(q.getPlanDateEnd() != null, SamplingPlan::getPlanDate, q.getPlanDateEnd())
                        .orderByDesc(SamplingPlan::getId));
        fillOrderCodes(page.getRecords());
        return page;
    }

    /** 列表/卡片上冗余委托单号 */
    public void fillOrderCodes(List<SamplingPlan> plans) {
        if (plans.isEmpty()) {
            return;
        }
        Set<Long> ids = plans.stream().map(SamplingPlan::getOrderId).collect(Collectors.toSet());
        Map<Long, String> codes = orderMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(EntrustOrder::getId, EntrustOrder::getCode));
        plans.forEach(p -> p.setOrderCode(codes.get(p.getOrderId())));
    }

    public PlanDetailVO detail(Long id) {
        SamplingPlan plan = mustGet(id);
        PlanDetailVO vo = new PlanDetailVO();
        vo.setPlan(plan);
        EntrustOrder order = orderMapper.selectById(plan.getOrderId());
        if (order != null) {
            vo.setOrderCode(order.getCode());
            vo.setOrderStatus(order.getStatus());
        }
        vo.setPoints(planPointMapper.selectList(Wrappers.<SamplingPlanPoint>lambdaQuery()
                .eq(SamplingPlanPoint::getPlanId, id).orderByAsc(SamplingPlanPoint::getSortNo)));
        vo.setItems(planItemMapper.selectList(Wrappers.<SamplingPlanItem>lambdaQuery()
                .eq(SamplingPlanItem::getPlanId, id).orderByAsc(SamplingPlanItem::getSortNo)));
        vo.setEquipments(planEquipmentMapper.selectList(Wrappers.<SamplingPlanEquipment>lambdaQuery()
                .eq(SamplingPlanEquipment::getPlanId, id)));
        vo.setTasks(taskMapper.selectList(Wrappers.<SamplingTask>lambdaQuery()
                .eq(SamplingTask::getPlanId, id).orderByDesc(SamplingTask::getId)));
        return vo;
    }

    public SamplingPlan mustGet(Long id) {
        SamplingPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return plan;
    }

    public List<SamplingPlanPoint> points(Long planId) {
        return planPointMapper.selectList(Wrappers.<SamplingPlanPoint>lambdaQuery()
                .eq(SamplingPlanPoint::getPlanId, planId).orderByAsc(SamplingPlanPoint::getSortNo));
    }

    public List<SamplingPlanItem> items(Long planId) {
        return planItemMapper.selectList(Wrappers.<SamplingPlanItem>lambdaQuery()
                .eq(SamplingPlanItem::getPlanId, planId).orderByAsc(SamplingPlanItem::getSortNo));
    }

    public List<SamplingPlanEquipment> equipments(Long planId) {
        List<SamplingPlanEquipment> list = planEquipmentMapper.selectList(
                Wrappers.<SamplingPlanEquipment>lambdaQuery().eq(SamplingPlanEquipment::getPlanId, planId));
        return list == null ? Collections.emptyList() : list;
    }
}
