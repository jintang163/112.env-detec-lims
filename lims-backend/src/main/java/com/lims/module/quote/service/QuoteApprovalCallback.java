package com.lims.module.quote.service;

import com.lims.module.approval.service.ApprovalCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuoteApprovalCallback implements ApprovalCallback {

    private final QuoteService quoteService;

    @Override
    public String bizType() {
        return QuoteService.BIZ_TYPE;
    }

    @Override
    public void onApproved(Long bizId, Long instanceId) {
        quoteService.onApproved(bizId);
    }

    @Override
    public void onRejected(Long bizId, Long instanceId, String comment) {
        quoteService.onRejected(bizId);
    }
}
