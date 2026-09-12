package com.lims.module.contract.service;

import com.lims.module.approval.service.ApprovalCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContractApprovalCallback implements ApprovalCallback {

    private final ContractService contractService;

    @Override
    public String bizType() {
        return ContractService.BIZ_TYPE;
    }

    @Override
    public void onApproved(Long bizId, Long instanceId) {
        contractService.onApproved(bizId, instanceId);
    }

    @Override
    public void onRejected(Long bizId, Long instanceId, String comment) {
        contractService.onRejected(bizId, instanceId, comment);
    }
}
