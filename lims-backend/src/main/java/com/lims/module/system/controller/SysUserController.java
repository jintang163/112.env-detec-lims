package com.lims.module.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lims.common.core.Result;
import com.lims.module.system.entity.SysUser;
import com.lims.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统用户精简接口(供下拉选择: 客户分配、审批人等)
 */
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserMapper userMapper;

    @GetMapping("/options")
    public Result<List<Map<String, Object>>> options(@RequestParam(required = false) String roleCode) {
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId));
        List<Map<String, Object>> all = users.stream()
                .map(u -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("userId", u.getId());
                    m.put("realName", u.getRealName());
                    m.put("username", u.getUsername());
                    m.put("phone", u.getPhone());
                    return m;
                }).collect(Collectors.toList());
        if (roleCode == null || roleCode.isEmpty()) {
            return Result.ok(all);
        }
        List<Long> ids = userMapper.selectUserIdsByRole(roleCode);
        return Result.ok(all.stream().filter(m -> ids.contains(m.get("userId"))).collect(Collectors.toList()));
    }
}
