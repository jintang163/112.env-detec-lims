package com.lims.module.system.controller;

import com.lims.common.core.Result;
import com.lims.module.system.entity.SysDictData;
import com.lims.module.system.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping("/{code}")
    public Result<List<SysDictData>> items(@PathVariable String code) {
        return Result.ok(dictService.items(code));
    }
}
