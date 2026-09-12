package com.lims.module.entrust.service;

import com.lims.module.approval.service.ApprovalCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntrustSubcontractCallback implements ApprovalCallback {

    private final EntrustSubcontractService service;

    @Override
    public String bizType() {
        return EntrustSubcontractService.BIZ_TYPE;
    }

    @Override
    public void onApproved(Long bizId, Long instanceId) {
        service.onApproved(bizId);
    }

    @Override
    public void onRejected(Long bizId, Long instanceId, String comment) {
        service.onRejected(bizId);
    }
}
