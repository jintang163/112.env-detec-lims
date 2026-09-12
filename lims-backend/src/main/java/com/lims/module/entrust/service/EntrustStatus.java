package com.lims.module.entrust.service;

import java.util.Map;
import java.util.Set;

/**
 * 委托单状态机
 * 草稿 DRAFT → 评审中 REVIEWING →(通过) 已受理 ACCEPTED → 采样中 SAMPLING
 *      → 检测中 TESTING → 报告中 REPORTING → 已完成 COMPLETED
 * 评审驳回 REVIEW_REJECTED 可修改后重新提交; 任意在途状态可 CANCELLED。
 */
public final class EntrustStatus {

    public static final String DRAFT = "DRAFT";
    public static final String REVIEWING = "REVIEWING";
    public static final String REVIEW_REJECTED = "REVIEW_REJECTED";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String SAMPLING = "SAMPLING";
    public static final String TESTING = "TESTING";
    public static final String REPORTING = "REPORTING";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    /** 动作 -> (前置状态集合 -> 目标状态) */
    public static final Map<String, Transition> ACTIONS = Map.of(
            "SUBMIT", new Transition(Set.of(DRAFT, REVIEW_REJECTED), REVIEWING),
            // 评审通过节点全部通过时直接受理
            "REVIEW_APPROVE", new Transition(Set.of(REVIEWING), ACCEPTED),
            "REVIEW_REJECT", new Transition(Set.of(REVIEWING), REVIEW_REJECTED),
            "START_SAMPLING", new Transition(Set.of(ACCEPTED), SAMPLING),
            "START_TESTING", new Transition(Set.of(SAMPLING), TESTING),
            "START_REPORT", new Transition(Set.of(TESTING), REPORTING),
            "COMPLETE", new Transition(Set.of(REPORTING), COMPLETED),
            "CANCEL", new Transition(Set.of(DRAFT, REVIEW_REJECTED, ACCEPTED, SAMPLING, TESTING, REPORTING), CANCELLED)
    );

    public static String targetOf(String action, String currentStatus) {
        Transition t = ACTIONS.get(action);
        if (t == null || !t.from.contains(currentStatus)) {
            throw new com.lims.common.core.BusinessException(
                    com.lims.common.core.ResultCode.STATUS_NOT_ALLOWED.getCode(),
                    "当前状态[" + currentStatus + "]不允许执行操作[" + action + "]");
        }
        return t.to;
    }

    private static class Transition {
        final Set<String> from;
        final String to;
        Transition(Set<String> from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    private EntrustStatus() {
    }
}
