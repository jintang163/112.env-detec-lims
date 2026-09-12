package com.lims.module.contract.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.common.util.CodeGenerator;
import com.lims.module.approval.service.ApprovalService;
import com.lims.module.contract.entity.Contract;
import com.lims.module.contract.entity.ContractChange;
import com.lims.module.contract.mapper.ContractChangeMapper;
import com.lims.module.contract.mapper.ContractMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 合同变更: 变更单审批留痕, 通过后合同状态置 CHANGED。
 */
@Service
@RequiredArgsConstructor
public class ContractChangeService {

    public static final String BIZ_TYPE = "CONTRACT_CHANGE";

    private final ContractChangeMapper changeMapper;
    private final ContractMapper contractMapper;
    private final ApprovalService approvalService;
    private final CodeGenerator codeGenerator;

    @Transactional
    public Long apply(ContractChange change) {
        Contract c = contractMapper.selectById(change.getContractId());
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!"APPROVED".equals(c.getStatus()) && !"EXECUTING".equals(c.getStatus()) && !"CHANGED".equals(c.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "仅已审批/履约中的合同可发起变更");
        }
        change.setChangeNo("BG" + codeGenerator.next("", false).replace("-", ""));
        change.setStatus("APPROVING");
        changeMapper.insert(change);

        SecurityUtils.LoginUser u = SecurityUtils.current();
        Long instId = approvalService.start(BIZ_TYPE, change.getId(),
                "合同变更: " + c.getName() + " " + change.getChangeNo(),
                u.getUserId(), u.getRealName());
        change.setApprovalId(instId);
        changeMapper.updateById(change);
        return change.getId();
    }

    @Transactional
    public void onApproved(Long changeId) {
        ContractChange change = changeMapper.selectById(changeId);
        if (change == null) {
            return;
        }
        change.setStatus("APPROVED");
        changeMapper.updateById(change);

        Contract c = contractMapper.selectById(change.getContractId());
        if (c != null) {
            c.setStatus("CHANGED");
            // 金额变更同步合同金额
            if ("AMOUNT".equals(change.getChangeType())) {
                try {
                    c.setAmount(new java.math.BigDecimal(change.getAfterContent().trim()));
                } catch (Exception ignored) {
                }
            }
            contractMapper.updateById(c);
        }
    }

    @Transactional
    public void onRejected(Long changeId) {
        ContractChange change = changeMapper.selectById(changeId);
        if (change != null) {
            change.setStatus("REJECTED");
            changeMapper.updateById(change);
        }
    }

    public List<ContractChange> list(Long contractId) {
        return changeMapper.selectList(Wrappers.<ContractChange>lambdaQuery()
                .eq(ContractChange::getContractId, contractId)
                .orderByDesc(ContractChange::getId));
    }
}
