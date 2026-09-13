package com.lims.module.approval.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批回调注册表: bizType -> 业务回调实现。
 * 回调实现依赖各自业务 Service, 而业务 Service 又依赖 ApprovalService,
 * 因此回调列表用 ObjectProvider 延迟解析, 避免启动期构造器循环依赖。
 */
@Component
public class ApprovalCallbackRegistry {

    private final ObjectProvider<List<ApprovalCallback>> callbacksProvider;
    private volatile Map<String, ApprovalCallback> map;

    public ApprovalCallbackRegistry(ObjectProvider<List<ApprovalCallback>> callbacksProvider) {
        this.callbacksProvider = callbacksProvider;
    }

    private Map<String, ApprovalCallback> map() {
        if (map == null) {
            synchronized (this) {
                if (map == null) {
                    Map<String, ApprovalCallback> m = new HashMap<>();
                    List<ApprovalCallback> callbacks = callbacksProvider.getIfAvailable();
                    if (callbacks != null) {
                        callbacks.forEach(cb -> m.put(cb.bizType(), cb));
                    }
                    map = m;
                }
            }
        }
        return map;
    }

    public ApprovalCallback get(String bizType) {
        return map().get(bizType);
    }
}
