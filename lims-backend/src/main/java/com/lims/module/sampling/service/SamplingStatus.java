package com.lims.module.sampling.service;

/**
 * 现场采样模块状态常量:
 * 计划 DRAFT→ISSUED→(CANCELLED);
 * 任务 ASSIGNED→SUBMITTED→HANDED (或 CANCELLED);
 * 样品 COLLECTED→RECEIVED;
 * 交接 PENDING→CONFIRMED/REJECTED。
 */
public final class SamplingStatus {

    private SamplingStatus() {
    }

    // 采样计划
    public static final String PLAN_DRAFT = "DRAFT";
    public static final String PLAN_ISSUED = "ISSUED";
    public static final String PLAN_CANCELLED = "CANCELLED";

    // 采样任务
    public static final String TASK_ASSIGNED = "ASSIGNED";
    public static final String TASK_SUBMITTED = "SUBMITTED";
    public static final String TASK_HANDED = "HANDED";
    public static final String TASK_CANCELLED = "CANCELLED";

    // 现场样品
    public static final String SAMPLE_COLLECTED = "COLLECTED";
    public static final String SAMPLE_RECEIVED = "RECEIVED";

    // 交接单
    public static final String HANDOVER_PENDING = "PENDING";
    public static final String HANDOVER_CONFIRMED = "CONFIRMED";
    public static final String HANDOVER_REJECTED = "REJECTED";
}
