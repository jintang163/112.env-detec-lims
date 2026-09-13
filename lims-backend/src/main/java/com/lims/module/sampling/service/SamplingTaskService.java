package com.lims.module.sampling.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.module.entrust.entity.EntrustOrder;
import com.lims.module.entrust.mapper.EntrustOrderMapper;
import com.lims.module.sampling.dto.TaskDetailVO;
import com.lims.module.sampling.dto.TaskQueryDTO;
import com.lims.module.sampling.entity.SamplingPlan;
import com.lims.module.sampling.entity.SamplingTask;
import com.lims.module.sampling.mapper.SamplingPlanMapper;
import com.lims.module.sampling.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 采样任务查询与任务详情整包(移动端离线下载)。
 */
@Service
@RequiredArgsConstructor
public class SamplingTaskService {

    private final SamplingTaskMapper taskMapper;
    private final SamplingPlanMapper planMapper;
    private final SamplingPlanService planService;
    private final EntrustOrderMapper orderMapper;
    private final FieldSampleService fieldSampleService;

    public SamplingTask mustGet(Long id) {
        SamplingTask t = taskMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return t;
    }

    public Page<SamplingTask> page(TaskQueryDTO q) {
        Page<SamplingTask> page = taskMapper.selectPage(new Page<>(q.getCurrent(), q.getSize()),
                Wrappers.<SamplingTask>lambdaQuery()
                        .and(StringUtils.hasText(q.getKeyword()),
                                w -> w.like(SamplingTask::getCode, q.getKeyword())
                                        .or().like(SamplingTask::getAssigneeName, q.getKeyword()))
                        .eq(StringUtils.hasText(q.getStatus()), SamplingTask::getStatus, q.getStatus())
                        .eq(q.getAssigneeId() != null, SamplingTask::getAssigneeId, q.getAssigneeId())
                        .eq(q.getPlanId() != null, SamplingTask::getPlanId, q.getPlanId())
                        .eq(q.getOrderId() != null, SamplingTask::getOrderId, q.getOrderId())
                        .orderByDesc(SamplingTask::getId));
        fillRefs(page.getRecords());
        return page;
    }

    /** 移动端: 派给我的任务(默认不显示已取消) */
    public Page<SamplingTask> myTasks(TaskQueryDTO q, Long userId) {
        Page<SamplingTask> page = taskMapper.selectPage(new Page<>(q.getCurrent(), q.getSize()),
                Wrappers.<SamplingTask>lambdaQuery()
                        .eq(SamplingTask::getAssigneeId, userId)
                        .eq(StringUtils.hasText(q.getStatus()), SamplingTask::getStatus, q.getStatus())
                        .ne(!StringUtils.hasText(q.getStatus()), SamplingTask::getStatus, SamplingStatus.TASK_CANCELLED)
                        .orderByDesc(SamplingTask::getId));
        fillRefs(page.getRecords());
        return page;
    }

    /** 填充冗余列: 计划标题/委托单号/样品数 */
    public void fillRefs(List<SamplingTask> tasks) {
        if (tasks.isEmpty()) {
            return;
        }
        Set<Long> planIds = tasks.stream().map(SamplingTask::getPlanId).collect(Collectors.toSet());
        Map<Long, SamplingPlan> plans = planMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(SamplingPlan::getId, p -> p));
        Set<Long> orderIds = tasks.stream().map(SamplingTask::getOrderId).collect(Collectors.toSet());
        Map<Long, EntrustOrder> orders = orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(EntrustOrder::getId, o -> o));
        for (SamplingTask t : tasks) {
            SamplingPlan p = plans.get(t.getPlanId());
            if (p != null) {
                t.setPlanTitle(p.getTitle());
            }
            EntrustOrder o = orders.get(t.getOrderId());
            if (o != null) {
                t.setOrderCode(o.getCode());
            }
            t.setSampleCount((int) fieldSampleService.countByTask(t.getId()));
        }
    }

    /** 任务详情整包: 任务+计划+点位+检测项+设备+已采样品(照片), 供移动端离线下载 */
    public TaskDetailVO detail(Long id) {
        return assemble(mustGet(id));
    }

    /** 移动端任务详情: 仅任务采样员本人(或管理员)可见, 防止改ID越权读取他人任务 */
    public TaskDetailVO mobileDetail(Long id) {
        SamplingTask task = mustGet(id);
        SecurityUtils.checkOwnerOrAdmin(task.getAssigneeId(), "仅任务采样员可查看该任务");
        return assemble(task);
    }

    private TaskDetailVO assemble(SamplingTask task) {
        TaskDetailVO vo = new TaskDetailVO();
        vo.setTask(task);
        SamplingPlan plan = planService.mustGet(task.getPlanId());
        vo.setPlan(plan);
        EntrustOrder order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            vo.setOrderCode(order.getCode());
            vo.setOrderTitle(order.getTitle());
        }
        vo.setPoints(planService.points(plan.getId()));
        vo.setItems(planService.items(plan.getId()));
        vo.setEquipments(planService.equipments(plan.getId()));
        vo.setSamples(fieldSampleService.listVOByTask(task.getId()));
        return vo;
    }
}
