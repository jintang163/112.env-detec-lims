package com.lims.module.approval.service;

/**
 * 业务模块实现该接口接收审批结果回调。
 * 阶段一为轻量内置审批流; 阶段五可改为 Camunda 监听器, 业务侧无需改动。
 */
public interface ApprovalCallback {

    /** 关心的业务类型, 如 CONTRACT/QUOTE/ENTRUST_REVIEW/ADJUSTMENT/SUBCONTRACT/CONTRACT_CHANGE */
    String bizType();

    /** 全部节点通过 */
    void onApproved(Long bizId, Long instanceId);

    /** 某一节点驳回 */
    void onRejected(Long bizId, Long instanceId, String comment);
}
