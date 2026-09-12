package com.lims.module.entrust.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.module.approval.service.ApprovalService;
import com.lims.module.entrust.entity.EntrustItem;
import com.lims.module.entrust.entity.EntrustOrder;
import com.lims.module.entrust.entity.EntrustSubcontract;
import com.lims.module.entrust.mapper.EntrustItemMapper;
import com.lims.module.entrust.mapper.EntrustSubcontractMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分包管理: 因资质能力不足/设备占用等将项目或整单分包给合作实验室,
 * 需检测主管审批分包方资质(CMA), 通过后委托单标记分包。
 */
@Service
@RequiredArgsConstructor
public class EntrustSubcontractService {

    public static final String BIZ_TYPE = "SUBCONTRACT";

    private final EntrustSubcontractMapper subcontractMapper;
    private final EntrustItemMapper itemMapper;
    private final EntrustOrderService orderService;
    private final com.lims.module.entrust.mapper.EntrustOrderMapper orderMapper;
    private final ApprovalService approvalService;

    @Transactional
    public Long apply(EntrustSubcontract s) {
        EntrustOrder o = orderService.mustGet(s.getOrderId());
        if (s.getSubcontractor() == null || s.getSubcontractor().trim().isEmpty()) {
            throw new BusinessException("请填写分包方");
        }
        if (s.getQualCert() == null || s.getQualCert().trim().isEmpty()) {
            throw new BusinessException("请填写分包方CMA资质证书号");
        }
        s.setStatus("APPROVING");
        subcontractMapper.insert(s);

        SecurityUtils.LoginUser u = SecurityUtils.current();
        Long instId = approvalService.start(BIZ_TYPE, s.getId(),
                "分包审批: " + o.getCode() + " -> " + s.getSubcontractor(),
                u.getUserId(), u.getRealName());
        s.setApprovalId(instId);
        subcontractMapper.updateById(s);
        return s.getId();
    }

    @Transactional
    public void onApproved(Long subId) {
        EntrustSubcontract s = subcontractMapper.selectById(subId);
        if (s == null) {
            return;
        }
        s.setStatus("APPROVED");
        subcontractMapper.updateById(s);

        EntrustOrder o = orderService.mustGet(s.getOrderId());
        o.setHasSubcontract(1);
        orderMapper.updateById(o);
        if (s.getItemId() != null) {
            EntrustItem item = itemMapper.selectById(s.getItemId());
            if (item != null) {
                item.setIsSubcontract(1);
                itemMapper.updateById(item);
            }
        } else {
            // 整单分包: 所有项目标记
            List<EntrustItem> items = itemMapper.selectList(Wrappers.<EntrustItem>lambdaQuery()
                    .eq(EntrustItem::getOrderId, s.getOrderId()));
            items.forEach(it -> it.setIsSubcontract(1));
            items.forEach(itemMapper::updateById);
        }
    }

    @Transactional
    public void onRejected(Long subId) {
        EntrustSubcontract s = subcontractMapper.selectById(subId);
        if (s != null) {
            s.setStatus("REJECTED");
            subcontractMapper.updateById(s);
        }
    }

    public List<EntrustSubcontract> list(Long orderId) {
        return subcontractMapper.selectList(Wrappers.<EntrustSubcontract>lambdaQuery()
                .eq(EntrustSubcontract::getOrderId, orderId)
                .orderByDesc(EntrustSubcontract::getId));
    }
}
