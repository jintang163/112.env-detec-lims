package com.lims.module.approval.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.ws.NotifyService;
import com.lims.module.approval.entity.*;
import com.lims.module.approval.mapper.*;
import com.lims.module.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 轻量顺序审批流引擎。
 * 流程定义在 biz_approval_flow(按 biz_type + node_seq);
 * 待办按角色派发, 同角色任一用户审批即完成该节点;
 * 结果通过 {@link ApprovalCallback} 回写业务单据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalInstanceMapper instanceMapper;
    private final ApprovalTaskMapper taskMapper;
    private final ApprovalRecordMapper recordMapper;
    private final ApprovalCallbackRegistry registry;
    private final NotifyService notifyService;
    private final AuthService authService;

    /**
     * 发起审批
     *
     * @return 审批实例ID
     */
    @Transactional
    public Long start(String bizType, Long bizId, String title, Long initiatorId, String initiatorName) {
        List<ApprovalFlow> nodes = flowMapper.selectList(Wrappers.<ApprovalFlow>lambdaQuery()
                .eq(ApprovalFlow::getBizType, bizType)
                .orderByAsc(ApprovalFlow::getNodeSeq));
        if (nodes.isEmpty()) {
            throw new BusinessException("未配置[" + bizType + "]审批流");
        }

        ApprovalInstance inst = new ApprovalInstance();
        inst.setBizType(bizType);
        inst.setBizId(bizId);
        inst.setTitle(title);
        inst.setStatus("PENDING");
        inst.setCurrentSeq(nodes.get(0).getNodeSeq());
        inst.setInitiatorId(initiatorId);
        inst.setInitiatorName(initiatorName);
        inst.setStartedAt(LocalDateTime.now());
        instanceMapper.insert(inst);

        createTaskAndNotify(inst, nodes.get(0));
        return inst.getId();
    }

    /**
     * 审批通过/驳回
     */
    @Transactional
    public void act(Long taskId, boolean approve, String comment, Long approverId, String approverName,
                    List<String> approverRoles) {
        ApprovalTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.APPROVAL_TASK_DONE);
        }
        if (approverRoles == null || !approverRoles.contains(task.getRoleCode())) {
            // 管理员可兜底审批
            if (approverRoles == null || !approverRoles.contains("ROLE_ADMIN")) {
                throw new BusinessException(ResultCode.NOT_APPROVER);
            }
        }

        ApprovalInstance inst = instanceMapper.selectById(task.getInstanceId());
        if (inst == null || !"PENDING".equals(inst.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED);
        }

        task.setStatus(approve ? "APPROVED" : "REJECTED");
        task.setAssigneeId(approverId);
        task.setAssigneeName(approverName);
        task.setComment(comment);
        task.setActedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        ApprovalRecord rec = new ApprovalRecord();
        rec.setInstanceId(inst.getId());
        rec.setNodeSeq(task.getNodeSeq());
        rec.setNodeName(task.getNodeName());
        rec.setApproverId(approverId);
        rec.setApproverName(approverName);
        rec.setAction(approve ? "APPROVE" : "REJECT");
        rec.setComment(comment);
        recordMapper.insert(rec);

        if (!approve) {
            inst.setStatus("REJECTED");
            inst.setFinishedAt(LocalDateTime.now());
            instanceMapper.updateById(inst);
            fireReject(inst, comment);
            return;
        }

        // 找下一节点
        ApprovalFlow next = flowMapper.selectOne(Wrappers.<ApprovalFlow>lambdaQuery()
                .eq(ApprovalFlow::getBizType, inst.getBizType())
                .gt(ApprovalFlow::getNodeSeq, task.getNodeSeq())
                .orderByAsc(ApprovalFlow::getNodeSeq)
                .last("limit 1"));
        if (next == null) {
            inst.setStatus("APPROVED");
            inst.setFinishedAt(LocalDateTime.now());
            instanceMapper.updateById(inst);
            fireApproved(inst);
        } else {
            inst.setCurrentSeq(next.getNodeSeq());
            instanceMapper.updateById(inst);
            createTaskAndNotify(inst, next);
        }
    }

    public List<java.util.Map<String, Object>> myTodo(Long userId, List<String> roles) {
        List<ApprovalTask> tasks = myTodoTasks(userId, roles);
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (ApprovalTask t : tasks) {
            ApprovalInstance inst = instanceMapper.selectById(t.getInstanceId());
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", t.getId());
            m.put("instanceId", t.getInstanceId());
            m.put("nodeSeq", t.getNodeSeq());
            m.put("nodeName", t.getNodeName());
            m.put("roleCode", t.getRoleCode());
            m.put("status", t.getStatus());
            m.put("createTime", t.getCreateTime());
            if (inst != null) {
                m.put("title", inst.getTitle());
                m.put("bizType", inst.getBizType());
                m.put("bizId", inst.getBizId());
                m.put("initiatorName", inst.getInitiatorName());
            }
            result.add(m);
        }
        return result;
    }

    public List<ApprovalTask> myTodoTasks(Long userId, List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return new ArrayList<>();
        }
        return taskMapper.selectList(Wrappers.<ApprovalTask>lambdaQuery()
                .eq(ApprovalTask::getStatus, "PENDING")
                .in(ApprovalTask::getRoleCode, roles)
                .orderByDesc(ApprovalTask::getId));
    }

    public long myTodoCount(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return 0;
        }
        return taskMapper.selectCount(Wrappers.<ApprovalTask>lambdaQuery()
                .eq(ApprovalTask::getStatus, "PENDING")
                .in(ApprovalTask::getRoleCode, roles));
    }

    public List<ApprovalRecord> timeline(Long instanceId) {
        return recordMapper.selectList(Wrappers.<ApprovalRecord>lambdaQuery()
                .eq(ApprovalRecord::getInstanceId, instanceId)
                .orderByAsc(ApprovalRecord::getNodeSeq, ApprovalRecord::getId));
    }

    public ApprovalInstance instanceByBiz(String bizType, Long bizId) {
        return instanceMapper.selectOne(Wrappers.<ApprovalInstance>lambdaQuery()
                .eq(ApprovalInstance::getBizType, bizType)
                .eq(ApprovalInstance::getBizId, bizId)
                .orderByDesc(ApprovalInstance::getId)
                .last("limit 1"));
    }

    private void createTaskAndNotify(ApprovalInstance inst, ApprovalFlow node) {
        ApprovalTask task = new ApprovalTask();
        task.setInstanceId(inst.getId());
        task.setNodeSeq(node.getNodeSeq());
        task.setNodeName(node.getNodeName());
        task.setRoleCode(node.getRoleCode());
        task.setStatus("PENDING");
        taskMapper.insert(task);

        // 通知该角色所有用户
        List<Long> userIds = authService.userIdsByRole(node.getRoleCode());
        Set<Long> dedup = new HashSet<>(userIds);
        dedup.forEach(uid -> notifyService.pushTodo(uid,
                "您有新的审批待办: " + inst.getTitle(),
                "节点: " + node.getNodeName(),
                inst.getBizType(), inst.getBizId()));
    }

    private void fireApproved(ApprovalInstance inst) {
        ApprovalCallback cb = registry.get(inst.getBizType());
        if (cb != null) {
            cb.onApproved(inst.getBizId(), inst.getId());
        }
        if (inst.getInitiatorId() != null) {
            notifyService.push(inst.getInitiatorId(), "审批已通过: " + inst.getTitle(),
                    "您发起的流程已全部审批通过", inst.getBizType(), inst.getBizId());
        }
    }

    private void fireReject(ApprovalInstance inst, String comment) {
        ApprovalCallback cb = registry.get(inst.getBizType());
        if (cb != null) {
            cb.onRejected(inst.getBizId(), inst.getId(), comment);
        }
        if (inst.getInitiatorId() != null) {
            notifyService.push(inst.getInitiatorId(), "审批已驳回: " + inst.getTitle(),
                    "驳回意见: " + (comment == null ? "" : comment), inst.getBizType(), inst.getBizId());
        }
    }
}
