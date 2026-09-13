package com.lims.module.sampling.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.common.util.CodeGenerator;
import com.lims.common.ws.NotifyService;
import com.lims.module.entrust.entity.EntrustOrder;
import com.lims.module.entrust.mapper.EntrustOrderMapper;
import com.lims.module.entrust.service.EntrustOrderService;
import com.lims.module.sampling.dto.HandoverCreateDTO;
import com.lims.module.sampling.dto.HandoverDetailVO;
import com.lims.module.sampling.dto.HandoverQueryDTO;
import com.lims.module.sampling.dto.SampleVO;
import com.lims.module.sampling.entity.FieldSample;
import com.lims.module.sampling.entity.SampleHandover;
import com.lims.module.sampling.entity.SamplingPlan;
import com.lims.module.sampling.entity.SamplingTask;
import com.lims.module.sampling.mapper.FieldSampleMapper;
import com.lims.module.sampling.mapper.SampleHandoverMapper;
import com.lims.module.sampling.mapper.SamplingPlanMapper;
import com.lims.module.sampling.mapper.SamplingTaskMapper;
import com.lims.module.system.entity.SysFile;
import com.lims.module.system.mapper.SysFileMapper;
import com.lims.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 样品交接: 采样员移动端发起 -> 样品管理员PC端接收确认 -> 委托单自动推进检测中。
 */
@Service
@RequiredArgsConstructor
public class HandoverService {

    private final SampleHandoverMapper handoverMapper;
    private final SamplingTaskMapper taskMapper;
    private final SamplingPlanMapper planMapper;
    private final EntrustOrderMapper orderMapper;
    private final FieldSampleMapper sampleMapper;
    private final SysFileMapper fileMapper;
    private final SysUserMapper sysUserMapper;
    private final FieldSampleService fieldSampleService;
    private final EntrustOrderService entrustOrderService;
    private final CodeGenerator codeGenerator;
    private final NotifyService notifyService;

    /** 采样员发起交接(任务下全部已采集样品) */
    @Transactional
    public Long create(HandoverCreateDTO dto) {
        SamplingTask task = taskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BusinessException("采样任务不存在");
        }
        SecurityUtils.LoginUser me = SecurityUtils.current();
        SecurityUtils.checkOwnerOrAdmin(task.getAssigneeId(), "仅任务采样员可发起交接");
        List<FieldSample> samples = sampleMapper.selectList(Wrappers.<FieldSample>lambdaQuery()
                .eq(FieldSample::getTaskId, task.getId())
                .eq(FieldSample::getStatus, SamplingStatus.SAMPLE_COLLECTED));
        if (samples.isEmpty()) {
            throw new BusinessException("任务下没有可交接的已采集样品");
        }
        Long pending = handoverMapper.selectCount(Wrappers.<SampleHandover>lambdaQuery()
                .eq(SampleHandover::getTaskId, task.getId())
                .eq(SampleHandover::getStatus, SamplingStatus.HANDOVER_PENDING));
        if (pending != null && pending > 0) {
            throw new BusinessException("该任务已有待接收的交接单, 请勿重复发起");
        }

        SampleHandover h = new SampleHandover();
        h.setCode(codeGenerator.next("JJ", false));
        h.setTaskId(task.getId());
        h.setPlanId(task.getPlanId());
        h.setOrderId(task.getOrderId());
        h.setSampleCount(samples.size());
        h.setSampleStatus(StringUtils.hasText(dto.getSampleStatus()) ? dto.getSampleStatus() : "完好");
        h.setHandoverById(me.getUserId());
        h.setHandoverByName(me.getRealName());
        h.setHandoverAt(LocalDateTime.now());
        h.setSigFileId(dto.getSigFileId());
        h.setStatus(SamplingStatus.HANDOVER_PENDING);
        handoverMapper.insert(h);

        for (FieldSample s : samples) {
            s.setHandoverId(h.getId());
            sampleMapper.updateById(s);
        }
        task.setStatus(SamplingStatus.TASK_SUBMITTED);
        task.setSubmittedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        for (Long uid : sysUserMapper.selectUserIdsByRole("ROLE_SAMPLE_MANAGER")) {
            notifyService.pushTodo(uid, "待接收样品交接: " + h.getCode(),
                    task.getAssigneeName() + " 移交 " + samples.size() + " 个样品", "SAMPLE_HANDOVER", h.getId());
        }
        return h.getId();
    }

    /** 管理员接收: 样品RECEIVED + 任务HANDED + 委托单推进TESTING */
    @Transactional
    public void confirm(Long id, String remark) {
        SampleHandover h = mustGet(id);
        if (!SamplingStatus.HANDOVER_PENDING.equals(h.getStatus())) {
            throw new BusinessException("仅待接收交接单可确认");
        }
        SecurityUtils.LoginUser me = SecurityUtils.current();
        h.setStatus(SamplingStatus.HANDOVER_CONFIRMED);
        h.setReceiverId(me.getUserId());
        h.setReceiverName(me.getRealName());
        h.setReceiveAt(LocalDateTime.now());
        h.setReceiverRemark(remark);
        handoverMapper.updateById(h);

        List<FieldSample> samples = sampleMapper.selectList(Wrappers.<FieldSample>lambdaQuery()
                .eq(FieldSample::getHandoverId, id));
        for (FieldSample s : samples) {
            s.setStatus(SamplingStatus.SAMPLE_RECEIVED);
            sampleMapper.updateById(s);
        }
        SamplingTask task = taskMapper.selectById(h.getTaskId());
        task.setStatus(SamplingStatus.TASK_HANDED);
        taskMapper.updateById(task);

        // 委托单仍在采样中才推进(多计划场景下可能已进入检测)
        EntrustOrder order = orderMapper.selectById(h.getOrderId());
        if (order != null && "SAMPLING".equals(order.getStatus())) {
            entrustOrderService.progress(h.getOrderId(), "START_TESTING",
                    "样品交接完成, 接收人 " + me.getRealName());
        }

        if (h.getHandoverById() != null) {
            notifyService.push(h.getHandoverById(), "样品交接已接收: " + h.getCode(),
                    samples.size() + " 个样品已由 " + me.getRealName() + " 接收", "SAMPLE_HANDOVER", h.getId());
        }
    }

    /** 拒收: 释放样品交接标记, 任务退回已分配以便补采/重新交接 */
    @Transactional
    public void reject(Long id, String reason) {
        SampleHandover h = mustGet(id);
        if (!SamplingStatus.HANDOVER_PENDING.equals(h.getStatus())) {
            throw new BusinessException("仅待接收交接单可拒收");
        }
        SecurityUtils.LoginUser me = SecurityUtils.current();
        h.setStatus(SamplingStatus.HANDOVER_REJECTED);
        h.setReceiverId(me.getUserId());
        h.setReceiverName(me.getRealName());
        h.setReceiveAt(LocalDateTime.now());
        h.setReceiverRemark(reason);
        handoverMapper.updateById(h);

        List<FieldSample> samples = sampleMapper.selectList(Wrappers.<FieldSample>lambdaQuery()
                .eq(FieldSample::getHandoverId, id));
        for (FieldSample s : samples) {
            s.setHandoverId(null);
            sampleMapper.updateById(s);
        }
        SamplingTask task = taskMapper.selectById(h.getTaskId());
        task.setStatus(SamplingStatus.TASK_ASSIGNED);
        taskMapper.updateById(task);

        if (h.getHandoverById() != null) {
            notifyService.push(h.getHandoverById(), "样品交接被拒收: " + h.getCode(),
                    "原因: " + (StringUtils.hasText(reason) ? reason : "未填写"), "SAMPLE_HANDOVER", h.getId());
        }
    }

    public Page<SampleHandover> page(HandoverQueryDTO q) {
        Page<SampleHandover> p = handoverMapper.selectPage(new Page<>(q.getCurrent(), q.getSize()),
                Wrappers.<SampleHandover>lambdaQuery()
                        .and(StringUtils.hasText(q.getKeyword()),
                                w -> w.like(SampleHandover::getCode, q.getKeyword())
                                        .or().like(SampleHandover::getHandoverByName, q.getKeyword()))
                        .eq(StringUtils.hasText(q.getStatus()), SampleHandover::getStatus, q.getStatus())
                        .eq(q.getTaskId() != null, SampleHandover::getTaskId, q.getTaskId())
                        .orderByDesc(SampleHandover::getId));
        fillRefs(p.getRecords());
        return p;
    }

    public List<SampleHandover> myHandovers(Long userId) {
        List<SampleHandover> list = handoverMapper.selectList(Wrappers.<SampleHandover>lambdaQuery()
                .eq(SampleHandover::getHandoverById, userId)
                .orderByDesc(SampleHandover::getId));
        fillRefs(list);
        return list;
    }

    private void fillRefs(List<SampleHandover> list) {
        if (list.isEmpty()) {
            return;
        }
        Set<Long> taskIds = list.stream().map(SampleHandover::getTaskId).collect(Collectors.toSet());
        Map<Long, SamplingTask> tasks = taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(SamplingTask::getId, t -> t));
        Set<Long> planIds = list.stream().map(SampleHandover::getPlanId).collect(Collectors.toSet());
        Map<Long, SamplingPlan> plans = planMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(SamplingPlan::getId, p -> p));
        Set<Long> orderIds = list.stream().map(SampleHandover::getOrderId).collect(Collectors.toSet());
        Map<Long, EntrustOrder> orders = orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(EntrustOrder::getId, o -> o));
        list.forEach(h -> {
            SamplingTask t = tasks.get(h.getTaskId());
            if (t != null) {
                h.setTaskCode(t.getCode());
            }
            SamplingPlan p = plans.get(h.getPlanId());
            if (p != null) {
                h.setPlanTitle(p.getTitle());
            }
            EntrustOrder o = orders.get(h.getOrderId());
            if (o != null) {
                h.setOrderCode(o.getCode());
            }
        });
    }

    public HandoverDetailVO detail(Long id) {
        SampleHandover h = mustGet(id);
        HandoverDetailVO vo = new HandoverDetailVO();
        vo.setHandover(h);
        SamplingTask task = taskMapper.selectById(h.getTaskId());
        if (task != null) {
            vo.setTaskCode(task.getCode());
        }
        SamplingPlan plan = planMapper.selectById(h.getPlanId());
        if (plan != null) {
            vo.setPlanTitle(plan.getTitle());
        }
        if (h.getSigFileId() != null) {
            SysFile sig = fileMapper.selectById(h.getSigFileId());
            if (sig != null) {
                vo.setSigUrl(sig.getUrl());
            }
        }
        List<FieldSample> samples = sampleMapper.selectList(Wrappers.<FieldSample>lambdaQuery()
                .eq(FieldSample::getHandoverId, id)
                .orderByAsc(FieldSample::getId));
        // 拒收后样品 handover_id 被清空, 兜底展示该任务的全部样品
        if (samples.isEmpty()) {
            samples = fieldSampleService.listByTask(h.getTaskId());
        }
        vo.setSamples(fieldSampleService.wrapPhotos(samples));
        return vo;
    }

    public SampleHandover mustGet(Long id) {
        SampleHandover h = handoverMapper.selectById(id);
        if (h == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return h;
    }
}
