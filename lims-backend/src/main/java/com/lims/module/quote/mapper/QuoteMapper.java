package com.lims.module.quote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lims.module.quote.entity.Quote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuoteMapper extends BaseMapper<Quote> {
}
