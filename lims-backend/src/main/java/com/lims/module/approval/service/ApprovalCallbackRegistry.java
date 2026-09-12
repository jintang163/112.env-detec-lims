package com.lims.module.approval.service;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批回调注册表: bizType -> 业务回调实现
 */
@Component
public class ApprovalCallbackRegistry {

    private final List<ApprovalCallback> callbacks;
    private final Map<String, ApprovalCallback> map = new HashMap<>();

    public ApprovalCallbackRegistry(List<ApprovalCallback> callbacks) {
        this.callbacks = callbacks;
    }

    @PostConstruct
    public void init() {
        callbacks.forEach(cb -> map.put(cb.bizType(), cb));
    }

    public ApprovalCallback get(String bizType) {
        return map.get(bizType);
    }
}
