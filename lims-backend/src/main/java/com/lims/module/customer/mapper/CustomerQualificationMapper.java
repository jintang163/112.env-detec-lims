package com.lims.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lims.module.customer.entity.CustomerQualification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CustomerQualificationMapper extends BaseMapper<CustomerQualification> {

    /** 定时刷新过期状态 */
    @Update("UPDATE biz_customer_qualification SET valid_status = 3 " +
            "WHERE valid_to IS NOT NULL AND valid_to < CURDATE() AND valid_status <> 3")
    int markExpired();

    @Update("UPDATE biz_customer_qualification SET valid_status = 2 " +
            "WHERE valid_to IS NOT NULL AND valid_to BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
            "AND valid_status = 1")
    int markExpiring();

    @Select("SELECT * FROM biz_customer_qualification WHERE customer_id = #{customerId} ORDER BY valid_to")
    List<CustomerQualification> listByCustomer(@Param("customerId") Long customerId);
}
