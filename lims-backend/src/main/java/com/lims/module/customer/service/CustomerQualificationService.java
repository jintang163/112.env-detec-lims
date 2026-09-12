package com.lims.module.customer.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.module.customer.entity.CustomerQualification;
import com.lims.module.customer.mapper.CustomerQualificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerQualificationService {

    private static final int EXPIRING_DAYS = 30;

    private final CustomerQualificationMapper qualificationMapper;

    public List<CustomerQualification> list(Long customerId) {
        return qualificationMapper.listByCustomer(customerId);
    }

    @Transactional
    public Long save(CustomerQualification q) {
        q.setValidStatus(calcStatus(q.getValidTo()));
        if (q.getId() == null) {
            qualificationMapper.insert(q);
        } else {
            qualificationMapper.updateById(q);
        }
        return q.getId();
    }

    public void delete(Long id) {
        qualificationMapper.deleteById(id);
    }

    /** 即将过期(30天内)或已过期资质, 工作台预警用 */
    public List<CustomerQualification> expiringSoon() {
        qualificationMapper.markExpired();
        qualificationMapper.markExpiring();
        return qualificationMapper.selectList(Wrappers.<CustomerQualification>lambdaQuery()
                .in(CustomerQualification::getValidStatus, 2, 3)
                .orderByAsc(CustomerQualification::getValidTo)
                .last("limit 100"));
    }

    private Integer calcStatus(LocalDate validTo) {
        if (validTo == null) {
            return 1;
        }
        LocalDate now = LocalDate.now();
        if (validTo.isBefore(now)) {
            return 3;
        }
        if (!validTo.isAfter(now.plusDays(EXPIRING_DAYS))) {
            return 2;
        }
        return 1;
    }
}
