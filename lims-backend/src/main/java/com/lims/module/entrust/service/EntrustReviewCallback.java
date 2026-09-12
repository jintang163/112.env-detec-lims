package com.lims.module.entrust.service;

import com.lims.module.approval.service.ApprovalCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 委托单合同评审审批回调: 流程全部通过 = 评审通过并受理; 驳回 = 评审驳回。
 */
@Component
@RequiredArgsConstructor
public class EntrustReviewCallback implements ApprovalCallback {

    private final EntrustOrderService orderService;

    @Override
    public String bizType() {
        return EntrustOrderService.REVIEW_BIZ;
    }

    @Override
    public void onApproved(Long bizId, Long instanceId) {
        orderService.onReviewApproved(bizId, null);
    }

    @Override
    public void onRejected(Long bizId, Long instanceId, String comment) {
        orderService.onReviewRejected(bizId, comment);
    }
}
