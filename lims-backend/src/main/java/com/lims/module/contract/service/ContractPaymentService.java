package com.lims.module.contract.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.module.contract.entity.ContractPayment;
import com.lims.module.contract.mapper.ContractPaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 合同履约跟踪: 收款计划 / 实际收款 / 开票记录
 */
@Service
@RequiredArgsConstructor
public class ContractPaymentService {

    private final ContractPaymentMapper paymentMapper;
    private final ContractService contractService;

    public List<ContractPayment> list(Long contractId, Integer payType) {
        return paymentMapper.selectList(Wrappers.<ContractPayment>lambdaQuery()
                .eq(ContractPayment::getContractId, contractId)
                .eq(payType != null, ContractPayment::getPayType, payType)
                .orderByAsc(ContractPayment::getPlanDate, ContractPayment::getOccurDate)
                .orderByDesc(ContractPayment::getId));
    }

    @Transactional
    public Long save(ContractPayment p) {
        if (p.getId() == null) {
            paymentMapper.insert(p);
        } else {
            paymentMapper.updateById(p);
        }
        if (p.getPayType() != null && p.getPayType() != 1) {
            contractService.recalcAmounts(p.getContractId());
        }
        return p.getId();
    }

    @Transactional
    public void delete(Long id) {
        ContractPayment p = paymentMapper.selectById(id);
        if (p != null) {
            paymentMapper.deleteById(id);
            if (p.getPayType() != null && p.getPayType() != 1) {
                contractService.recalcAmounts(p.getContractId());
            }
        }
    }
}
