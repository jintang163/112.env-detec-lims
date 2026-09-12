package com.lims.module.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lims.module.contract.entity.Contract;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContractMapper extends BaseMapper<Contract> {
}
