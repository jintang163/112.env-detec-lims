package com.lims.module.entrust.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.module.approval.service.ApprovalService;
import com.lims.module.entrust.entity.EntrustAdjustment;
import com.lims.module.entrust.entity.EntrustOrder;
import com.lims.module.entrust.mapper.EntrustAdjustmentMapper;
import com.lims.module.entrust.mapper.EntrustOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 委托单调账(调增/调减优惠): 需主管+财务两级审批, 通过后回写委托单 adjusted_amount。
 */
@Service
@RequiredArgsConstructor
public class EntrustAdjustmentService {

    public static final String BIZ_TYPE = "ADJUSTMENT";

    private final EntrustAdjustmentMapper adjustmentMapper;
    private final EntrustOrderMapper orderMapper;
    private final EntrustOrderService orderService;
    private final ApprovalService approvalService;

    @Transactional
    public Long apply(EntrustAdjustment a) {
        EntrustOrder o = orderService.mustGet(a.getOrderId());
        if (o.getStatus().equals(EntrustStatus.COMPLETED) || o.getStatus().equals(EntrustStatus.CANCELLED)) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "已完成/取消的委托不能调账");
        }
        BigDecimal base = o.getAdjustedAmount() == null ? o.getTotalAmount() : o.getAdjustedAmount();
        BigDecimal delta = a.getAdjustAmount() == null ? BigDecimal.ZERO : a.getAdjustAmount();
        BigDecimal after = a.getAdjustType() == 1 ? base.add(delta) : base.subtract(delta);
        if (after.signum() < 0) {
            throw new BusinessException("调减后金额不能为负");
        }
        a.setBeforeAmount(base);
        a.setAfterAmount(after);
        a.setStatus("APPROVING");
        adjustmentMapper.insert(a);

        SecurityUtils.LoginUser u = SecurityUtils.current();
        Long instId = approvalService.start(BIZ_TYPE, a.getId(),
                "委托单调账: " + o.getCode() + (a.getAdjustType() == 1 ? " 调增" : " 调减")
                        + delta.toPlainString(),
                u.getUserId(), u.getRealName());
        a.setApprovalId(instId);
        adjustmentMapper.updateById(a);
        return a.getId();
    }

    @Transactional
    public void onApproved(Long adjustmentId) {
        EntrustAdjustment a = adjustmentMapper.selectById(adjustmentId);
        if (a == null) {
            return;
        }
        a.setStatus("APPROVED");
        adjustmentMapper.updateById(a);
        EntrustOrder o = orderService.mustGet(a.getOrderId());
        o.setAdjustedAmount(a.getAfterAmount());
        o.setAdjustmentReason(a.getReason());
        orderMapper.updateById(o);
    }

    @Transactional
    public void onRejected(Long adjustmentId) {
        EntrustAdjustment a = adjustmentMapper.selectById(adjustmentId);
        if (a != null) {
            a.setStatus("REJECTED");
            adjustmentMapper.updateById(a);
        }
    }

    public List<EntrustAdjustment> list(Long orderId) {
        return adjustmentMapper.selectList(Wrappers.<EntrustAdjustment>lambdaQuery()
                .eq(EntrustAdjustment::getOrderId, orderId)
                .orderByDesc(EntrustAdjustment::getId));
    }
}
