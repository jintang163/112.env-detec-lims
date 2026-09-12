package com.lims.module.entrust.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lims.module.entrust.entity.EntrustOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface EntrustOrderMapper extends BaseMapper<EntrustOrder> {

    /** 按状态统计(工作台) */
    @Select("SELECT status, COUNT(1) AS cnt FROM biz_entrust_order WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /** 本月委托量 */
    @Select("SELECT COUNT(1) FROM biz_entrust_order WHERE deleted = 0 " +
            "AND DATE_FORMAT(create_time,'%Y-%m') = DATE_FORMAT(CURDATE(),'%Y-%m')")
    long countCurrentMonth();

    /** 临期预警: 期望报告日 <= N天内且未完成 */
    @Select("SELECT * FROM biz_entrust_order WHERE deleted = 0 " +
            "AND status IN ('ACCEPTED','SAMPLING','TESTING','REPORTING') " +
            "AND expected_report_date IS NOT NULL " +
            "AND expected_report_date <= DATE_ADD(CURDATE(), INTERVAL #{days} DAY) " +
            "ORDER BY expected_report_date LIMIT #{limit}")
    List<EntrustOrder> expiringSoon(@Param("days") int days, @Param("limit") int limit);
}
