package com.lims.module.entrust.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.common.util.CodeGenerator;
import com.lims.module.approval.service.ApprovalService;
import com.lims.module.contract.entity.Contract;
import com.lims.module.contract.mapper.ContractMapper;
import com.lims.module.customer.entity.Customer;
import com.lims.module.customer.mapper.CustomerMapper;
import com.lims.module.entrust.dto.EntrustDetailVO;
import com.lims.module.entrust.dto.EntrustQueryDTO;
import com.lims.module.entrust.dto.EntrustSaveDTO;
import com.lims.module.entrust.entity.*;
import com.lims.module.entrust.mapper.*;
import com.lims.module.quote.entity.QuoteItem;
import com.lims.module.quote.mapper.QuoteItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntrustOrderService {

    public static final String REVIEW_BIZ = "ENTRUST_REVIEW";

    private final EntrustOrderMapper orderMapper;
    private final EntrustItemMapper itemMapper;
    private final EntrustSamplingPointMapper pointMapper;
    private final EntrustStatusLogMapper logMapper;
    private final CustomerMapper customerMapper;
    private final ContractMapper contractMapper;
    private final QuoteItemMapper quoteItemMapper;
    private final ApprovalService approvalService;
    private final CodeGenerator codeGenerator;
    private final com.lims.module.system.mapper.SysUserMapper sysUserMapper;

    @Transactional
    public Long save(EntrustSaveDTO dto) {
        EntrustOrder o = dto.getId() == null ? new EntrustOrder() : orderMapper.selectById(dto.getId());
        if (o == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (dto.getId() != null
                && !EntrustStatus.DRAFT.equals(o.getStatus())
                && !EntrustStatus.REVIEW_REJECTED.equals(o.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "仅草稿/评审驳回状态可修改");
        }
        if (customerMapper.selectById(dto.getCustomerId()) == null) {
            throw new BusinessException("客户不存在");
        }
        org.springframework.beans.BeanUtils.copyProperties(dto, o, "id", "items", "points");

        BigDecimal total = BigDecimal.ZERO;
        List<EntrustItem> items = dto.getItems() == null ? List.of() : dto.getItems();
        int idx = 1;
        for (EntrustItem it : items) {
            it.setSortNo(idx++);
            BigDecimal qty = it.getQty() == null ? BigDecimal.ONE : it.getQty();
            BigDecimal price = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
            if (it.getAmount() == null) {
                it.setAmount(qty.multiply(price));
            }
            total = total.add(it.getAmount());
            it.setIsSubcontract(it.getIsSubcontract() == null ? 0 : it.getIsSubcontract());
        }
        o.setTotalAmount(total);
        if (o.getAdjustedAmount() == null || o.getAdjustedAmount().signum() == 0) {
            o.setAdjustedAmount(total);
        }

        if (dto.getId() == null) {
            o.setCode(codeGenerator.next("WT-", true));
            o.setStatus(EntrustStatus.DRAFT);
            o.setHasSubcontract(0);
            orderMapper.insert(o);
            writeLog(o.getId(), null, EntrustStatus.DRAFT, "CREATE", "创建委托单");
        } else {
            orderMapper.updateById(o);
            itemMapper.delete(Wrappers.<EntrustItem>lambdaQuery().eq(EntrustItem::getOrderId, o.getId()));
            pointMapper.delete(Wrappers.<EntrustSamplingPoint>lambdaQuery().eq(EntrustSamplingPoint::getOrderId, o.getId()));
        }

        for (EntrustItem it : items) {
            it.setId(null);
            it.setOrderId(o.getId());
            itemMapper.insert(it);
        }
        int pidx = 1;
        if (dto.getPoints() != null) {
            for (EntrustSamplingPoint p : dto.getPoints()) {
                p.setId(null);
                p.setOrderId(o.getId());
                p.setSortNo(pidx++);
                pointMapper.insert(p);
            }
        }
        return o.getId();
    }

    /** 从已审批报价单一键带入项目与金额 */
    @Transactional
    public Long createFromQuote(Long quoteId, EntrustSaveDTO base) {
        List<QuoteItem> qItems = quoteItemMapper.selectList(Wrappers.<QuoteItem>lambdaQuery()
                .eq(QuoteItem::getQuoteId, quoteId).orderByAsc(QuoteItem::getSortNo));
        List<EntrustItem> items = qItems.stream().map(q -> {
            EntrustItem e = new EntrustItem();
            e.setItemName(q.getItemName());
            e.setStandardCode(q.getStandardCode());
            e.setSampleName(q.getSpec());
            e.setQty(q.getQty());
            e.setUnitPrice(q.getUnitPrice());
            e.setAmount(q.getAmount());
            e.setIsSubcontract(0);
            return e;
        }).collect(Collectors.toList());
        base.setItems(items);
        base.setQuoteId(quoteId);
        return save(base);
    }

    public EntrustDetailVO detail(Long id) {
        EntrustOrder o = mustGet(id);
        EntrustDetailVO vo = new EntrustDetailVO();
        vo.setOrder(o);
        vo.setItems(itemMapper.selectList(Wrappers.<EntrustItem>lambdaQuery()
                .eq(EntrustItem::getOrderId, id).orderByAsc(EntrustItem::getSortNo)));
        vo.setPoints(pointMapper.selectList(Wrappers.<EntrustSamplingPoint>lambdaQuery()
                .eq(EntrustSamplingPoint::getOrderId, id).orderByAsc(EntrustSamplingPoint::getSortNo)));
        Customer c = customerMapper.selectById(o.getCustomerId());
        if (c != null) {
            vo.setCustomerName(c.getName());
            vo.setCustomerLevel(c.getCustomerLevel());
        }
        if (o.getContractId() != null) {
            Contract ct = contractMapper.selectById(o.getContractId());
            if (ct != null) {
                vo.setContractCode(ct.getCode());
                vo.setContractName(ct.getName());
            }
        }
        if (o.getReviewerId() != null) {
            com.lims.module.system.entity.SysUser reviewer = sysUserMapper.selectById(o.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerName(reviewer.getRealName());
            }
        }
        return vo;
    }

    public Page<EntrustDetailVO> page(EntrustQueryDTO q) {
        SecurityUtils.LoginUser u = SecurityUtils.current();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EntrustOrder> wrap =
                Wrappers.<EntrustOrder>lambdaQuery()
                .and(StringUtils.hasText(q.getKeyword()),
                        w -> w.like(EntrustOrder::getTitle, q.getKeyword())
                                .or().like(EntrustOrder::getCode, q.getKeyword()))
                .eq(q.getCustomerId() != null, EntrustOrder::getCustomerId, q.getCustomerId())
                .eq(q.getContractId() != null, EntrustOrder::getContractId, q.getContractId())
                .eq(StringUtils.hasText(q.getStatus()), EntrustOrder::getStatus, q.getStatus())
                .eq(StringUtils.hasText(q.getUrgency()), EntrustOrder::getUrgency, q.getUrgency())
                .eq(StringUtils.hasText(q.getEntrustType()), EntrustOrder::getEntrustType, q.getEntrustType())
                .ge(q.getExpectDateStart() != null, EntrustOrder::getExpectedReportDate, q.getExpectDateStart())
                .le(q.getExpectDateEnd() != null, EntrustOrder::getExpectedReportDate, q.getExpectDateEnd())
                .eq(Boolean.TRUE.equals(q.getMine()), EntrustOrder::getCreateBy, u.getUsername())
                .orderByDesc(EntrustOrder::getId);
        Page<EntrustOrder> page = orderMapper.selectPage(new Page<>(q.getCurrent(), q.getSize()), wrap);
        Page<EntrustDetailVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        Set<Long> customerIds = page.getRecords().stream().map(EntrustOrder::getCustomerId).collect(Collectors.toSet());
        Map<Long, String> names = customerIds.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        result.setRecords(page.getRecords().stream().map(o -> {
            EntrustDetailVO vo = new EntrustDetailVO();
            vo.setOrder(o);
            vo.setCustomerName(names.get(o.getCustomerId()));
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    /** 提交合同评审 */
    @Transactional
    public void submitReview(Long id, String remark) {
        EntrustOrder o = mustGet(id);
        if (!EntrustStatus.DRAFT.equals(o.getStatus()) && !EntrustStatus.REVIEW_REJECTED.equals(o.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED);
        }
        long cnt = itemMapper.selectCount(Wrappers.<EntrustItem>lambdaQuery().eq(EntrustItem::getOrderId, id));
        if (cnt == 0) {
            throw new BusinessException("请先添加检测项目与检测标准");
        }
        if (o.getExpectedReportDate() == null) {
            throw new BusinessException("请填写期望报告时间");
        }
        transition(o, "SUBMIT", remark);
        SecurityUtils.LoginUser u = SecurityUtils.current();
        Long instId = approvalService.start(REVIEW_BIZ, id,
                "委托单合同评审: " + o.getTitle() + "(" + o.getCode() + ")",
                u.getUserId(), u.getRealName());
        o.setApprovalId(instId);
        orderMapper.updateById(o);
    }

    @Transactional
    public void onReviewApproved(Long id, String opinion) {
        EntrustOrder o = mustGet(id);
        SecurityUtils.LoginUser u = SecurityUtils.currentOrNull();
        o.setReviewerId(u == null ? o.getReviewerId() : u.getUserId());
        o.setReviewTime(LocalDateTime.now());
        o.setReviewOpinion(StringUtils.hasText(opinion) ? opinion
                : "资质有效、具备检测能力、方法标准现行、资源满足工期,评审通过。");
        o.setStatus(EntrustStatus.ACCEPTED);
        o.setAcceptedAt(LocalDateTime.now());
        orderMapper.updateById(o);
        writeLog(id, EntrustStatus.REVIEWING, EntrustStatus.ACCEPTED, "REVIEW_APPROVE",
                o.getReviewOpinion());
    }

    @Transactional
    public void onReviewRejected(Long id, String reason) {
        EntrustOrder o = mustGet(id);
        o.setStatus(EntrustStatus.REVIEW_REJECTED);
        o.setReviewOpinion(reason);
        orderMapper.updateById(o);
        writeLog(id, EntrustStatus.REVIEWING, EntrustStatus.REVIEW_REJECTED, "REVIEW_REJECT", reason);
    }

    /** 采样/检测/报告/完成 状态推进 */
    @Transactional
    public void progress(Long id, String action, String remark) {
        EntrustOrder o = mustGet(id);
        String from = o.getStatus();
        String to = EntrustStatus.targetOf(action, from);
        LocalDateTime now = LocalDateTime.now();
        switch (to) {
            case EntrustStatus.SAMPLING:
                o.setSamplingAt(now);
                break;
            case EntrustStatus.TESTING:
                o.setTestingAt(now);
                break;
            case EntrustStatus.REPORTING:
                o.setReportingAt(now);
                break;
            case EntrustStatus.COMPLETED:
                o.setCompletedAt(now);
                break;
            default:
        }
        o.setStatus(to);
        orderMapper.updateById(o);
        writeLog(id, from, to, action, remark);
    }

    @Transactional
    public void cancel(Long id, String reason) {
        EntrustOrder o = mustGet(id);
        String from = o.getStatus();
        EntrustStatus.targetOf("CANCEL", from);
        o.setStatus(EntrustStatus.CANCELLED);
        o.setCancelReason(reason);
        orderMapper.updateById(o);
        writeLog(id, from, EntrustStatus.CANCELLED, "CANCEL", reason);
    }

    /** 加急切换 */
    @Transactional
    public void toggleUrgent(Long id, boolean urgent, String reason) {
        EntrustOrder o = mustGet(id);
        o.setUrgency(urgent ? "URGENT" : "NORMAL");
        orderMapper.updateById(o);
        writeLog(id, o.getStatus(), o.getStatus(), urgent ? "MARK_URGENT" : "UNMARK_URGENT",
                (urgent ? "设为加急" : "取消加急") + (StringUtils.hasText(reason) ? ":" + reason : ""));
    }

    public List<EntrustStatusLog> timeline(Long id) {
        return logMapper.selectList(Wrappers.<EntrustStatusLog>lambdaQuery()
                .eq(EntrustStatusLog::getOrderId, id)
                .orderByAsc(EntrustStatusLog::getId));
    }

    public List<EntrustItem> items(Long id) {
        return itemMapper.selectList(Wrappers.<EntrustItem>lambdaQuery()
                .eq(EntrustItem::getOrderId, id).orderByAsc(EntrustItem::getSortNo));
    }

    public List<EntrustSamplingPoint> points(Long id) {
        return pointMapper.selectList(Wrappers.<EntrustSamplingPoint>lambdaQuery()
                .eq(EntrustSamplingPoint::getOrderId, id).orderByAsc(EntrustSamplingPoint::getSortNo));
    }

    /** 移动端现场采样: 追加一个采样点位(BD-09 经纬度) */
    @Transactional
    public Long addSamplingPoint(Long orderId, String name, BigDecimal lng, BigDecimal lat, String addrDesc) {
        mustGet(orderId);
        Long count = pointMapper.selectCount(Wrappers.<EntrustSamplingPoint>lambdaQuery()
                .eq(EntrustSamplingPoint::getOrderId, orderId));
        EntrustSamplingPoint p = new EntrustSamplingPoint();
        p.setOrderId(orderId);
        p.setName(name);
        p.setLng(lng);
        p.setLat(lat);
        p.setAddrDesc(addrDesc);
        p.setSortNo(count.intValue() + 1);
        pointMapper.insert(p);
        return p.getId();
    }

    private void transition(EntrustOrder o, String action, String remark) {
        String from = o.getStatus();
        String to = EntrustStatus.targetOf(action, from);
        o.setStatus(to);
        orderMapper.updateById(o);
        writeLog(o.getId(), from, to, action, remark);
    }

    private void writeLog(Long orderId, String from, String to, String action, String remark) {
        SecurityUtils.LoginUser u = SecurityUtils.currentOrNull();
        EntrustStatusLog l = new EntrustStatusLog();
        l.setOrderId(orderId);
        l.setFromStatus(from);
        l.setToStatus(to);
        l.setAction(action);
        l.setRemark(remark);
        l.setOperatorId(u == null ? null : u.getUserId());
        l.setOperatorName(u == null ? "system" : u.getRealName());
        logMapper.insert(l);
    }

    EntrustOrder mustGet(Long id) {
        EntrustOrder o = orderMapper.selectById(id);
        if (o == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return o;
    }
}
