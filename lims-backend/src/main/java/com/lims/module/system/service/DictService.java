package com.lims.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.module.system.entity.SysDictData;
import com.lims.module.system.mapper.SysDictDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictDataMapper dictDataMapper;

    public List<SysDictData> items(String dictCode) {
        return dictDataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictCode, dictCode)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSortNo));
    }
}
