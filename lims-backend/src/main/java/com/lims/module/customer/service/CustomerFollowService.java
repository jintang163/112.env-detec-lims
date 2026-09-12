package com.lims.module.customer.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.common.security.SecurityUtils;
import com.lims.module.customer.entity.Customer;
import com.lims.module.customer.entity.CustomerFollow;
import com.lims.module.customer.mapper.CustomerFollowMapper;
import com.lims.module.customer.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerFollowService {

    private final CustomerFollowMapper followMapper;
    private final CustomerMapper customerMapper;

    @Transactional
    public void add(CustomerFollow f) {
        SecurityUtils.LoginUser u = SecurityUtils.current();
        f.setOperatorId(u.getUserId());
        f.setOperatorName(u.getRealName());
        followMapper.insert(f);

        Customer c = customerMapper.selectById(f.getCustomerId());
        if (c != null) {
            c.setFollowCount((c.getFollowCount() == null ? 0 : c.getFollowCount()) + 1);
            c.setLastFollowAt(LocalDateTime.now());
            customerMapper.updateById(c);
        }
    }

    public List<CustomerFollow> list(Long customerId) {
        return followMapper.selectList(Wrappers.<CustomerFollow>lambdaQuery()
                .eq(CustomerFollow::getCustomerId, customerId)
                .orderByDesc(CustomerFollow::getId));
    }
}
