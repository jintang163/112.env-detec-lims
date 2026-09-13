package com.lims.module.quote.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.common.util.CodeGenerator;
import com.lims.module.approval.service.ApprovalService;
import com.lims.module.customer.entity.Customer;
import com.lims.module.customer.mapper.CustomerMapper;
import com.lims.module.quote.dto.QuoteQueryDTO;
import com.lims.module.quote.dto.QuoteSaveDTO;
import com.lims.module.quote.dto.QuoteVO;
import com.lims.module.quote.entity.Quote;
import com.lims.module.quote.entity.QuoteItem;
import com.lims.module.quote.mapper.QuoteItemMapper;
import com.lims.module.quote.mapper.QuoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuoteService {

    public static final String BIZ_TYPE = "QUOTE";

    private final QuoteMapper quoteMapper;
    private final QuoteItemMapper itemMapper;
    private final CustomerMapper customerMapper;
    private final PricingService pricingService;
    private final ApprovalService approvalService;
    private final CodeGenerator codeGenerator;

    @Transactional
    public Long save(QuoteSaveDTO dto) {
        Quote q = dto.getId() == null ? new Quote() : quoteMapper.selectById(dto.getId());
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (dto.getId() != null && !"DRAFT".equals(q.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "仅草稿状态报价可修改");
        }
        q.setTitle(dto.getTitle());
        q.setCustomerId(dto.getCustomerId());
        q.setPricingMode(dto.getPricingMode());
        q.setDiscountAmount(nz(dto.getDiscountAmount()));
        q.setUrgentFactor(dto.getUrgentFactor() == null ? BigDecimal.ONE : dto.getUrgentFactor());
        q.setValidUntil(dto.getValidUntil());
        q.setRemark(dto.getRemark());

        List<QuoteItem> items = dto.getItems() == null ? Collections.<QuoteItem>emptyList() : dto.getItems();
        int idx = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (QuoteItem item : items) {
            item.setSortNo(idx++);
            if ("RULE".equals(q.getPricingMode()) || item.getAmount() == null) {
                PricingService.PricingResult pr = pricingService.price(item, q.getUrgentFactor(), BigDecimal.ONE);
                item.setAmount(pr.getAmount());
                item.setFormula(pr.getExpression());
            }
            total = total.add(nz(item.getAmount()));
        }
        q.setTotalAmount(total);
        q.setFinalAmount(total.subtract(nz(dto.getDiscountAmount())).max(BigDecimal.ZERO));

        if (dto.getId() == null) {
            q.setCode(codeGenerator.next("BJ", false));
            q.setStatus("DRAFT");
            quoteMapper.insert(q);
        } else {
            quoteMapper.updateById(q);
            itemMapper.delete(Wrappers.<QuoteItem>lambdaQuery().eq(QuoteItem::getQuoteId, q.getId()));
        }
        for (QuoteItem item : items) {
            item.setId(null);
            item.setQuoteId(q.getId());
            itemMapper.insert(item);
        }
        return q.getId();
    }

    /** 仅试算不保存, 前端编辑时实时计价 */
    public List<QuoteItem> calculate(List<QuoteItem> items, BigDecimal urgentFactor) {
        items.forEach(item -> {
            PricingService.PricingResult pr = pricingService.price(item, urgentFactor, BigDecimal.ONE);
            item.setAmount(pr.getAmount());
            item.setFormula(pr.getExpression() + "  [" + pr.getRuleName() + "]");
        });
        return items;
    }

    @Transactional
    public void submit(Long id) {
        Quote q = mustGet(id);
        if (!"DRAFT".equals(q.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED);
        }
        Long count = itemMapper.selectCount(Wrappers.<QuoteItem>lambdaQuery().eq(QuoteItem::getQuoteId, id));
        if (count == 0) {
            throw new BusinessException("请先添加检测项目明细");
        }
        q.setStatus("APPROVING");
        quoteMapper.updateById(q);
        SecurityUtils.LoginUser u = SecurityUtils.current();
        Long instId = approvalService.start(BIZ_TYPE, id,
                "报价审批: " + q.getTitle() + "(" + q.getCode() + ")",
                u.getUserId(), u.getRealName());
        q.setApprovalId(instId);
        quoteMapper.updateById(q);
    }

    @Transactional
    public void onApproved(Long id) {
        Quote q = mustGet(id);
        q.setStatus("APPROVED");
        q.setApprovedAt(java.time.LocalDateTime.now());
        quoteMapper.updateById(q);
    }

    @Transactional
    public void onRejected(Long id) {
        Quote q = mustGet(id);
        q.setStatus("DRAFT");
        quoteMapper.updateById(q);
    }

    @Transactional
    public void voidQuote(Long id) {
        Quote q = mustGet(id);
        if ("APPROVED".equals(q.getStatus()) || "DRAFT".equals(q.getStatus())) {
            q.setStatus("VOID");
            quoteMapper.updateById(q);
        } else {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED);
        }
    }

    public QuoteVO detail(Long id) {
        Quote q = mustGet(id);
        QuoteVO vo = new QuoteVO();
        org.springframework.beans.BeanUtils.copyProperties(q, vo);
        vo.setItems(itemMapper.selectList(Wrappers.<QuoteItem>lambdaQuery()
                .eq(QuoteItem::getQuoteId, id).orderByAsc(QuoteItem::getSortNo)));
        Customer c = customerMapper.selectById(q.getCustomerId());
        if (c != null) {
            vo.setCustomerName(c.getName());
            vo.setCustomerLevel(c.getCustomerLevel());
        }
        return vo;
    }

    public Page<QuoteVO> page(QuoteQueryDTO qry) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Quote> wrap =
                Wrappers.<Quote>lambdaQuery()
                .and(StringUtils.hasText(qry.getKeyword()),
                        w -> w.like(Quote::getTitle, qry.getKeyword()).or().like(Quote::getCode, qry.getKeyword()))
                .eq(qry.getCustomerId() != null, Quote::getCustomerId, qry.getCustomerId())
                .eq(StringUtils.hasText(qry.getStatus()), Quote::getStatus, qry.getStatus())
                .orderByDesc(Quote::getId);
        Page<Quote> page = quoteMapper.selectPage(new Page<>(qry.getCurrent(), qry.getSize()), wrap);
        Page<QuoteVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<QuoteVO> vos = page.getRecords().stream().map(q -> {
            QuoteVO vo = new QuoteVO();
            org.springframework.beans.BeanUtils.copyProperties(q, vo);
            return vo;
        }).collect(Collectors.toList());
        Set<Long> ids = vos.stream().map(Quote::getCustomerId).collect(Collectors.toSet());
        if (!ids.isEmpty()) {
            Map<Long, Customer> map = customerMapper.selectBatchIds(ids).stream()
                    .collect(Collectors.toMap(Customer::getId, x -> x));
            vos.forEach(vo -> {
                Customer c = map.get(vo.getCustomerId());
                if (c != null) {
                    vo.setCustomerName(c.getName());
                    vo.setCustomerLevel(c.getCustomerLevel());
                }
            });
        }
        result.setRecords(vos);
        return result;
    }

    public List<QuoteItem> items(Long quoteId) {
        return itemMapper.selectList(Wrappers.<QuoteItem>lambdaQuery()
                .eq(QuoteItem::getQuoteId, quoteId).orderByAsc(QuoteItem::getSortNo));
    }

    private Quote mustGet(Long id) {
        Quote q = quoteMapper.selectById(id);
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return q;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
