package com.lims.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lims.module.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select({
            "<script>",
            "SELECT DISTINCT p.* FROM sys_permission p",
            "JOIN sys_role_permission rp ON rp.permission_id = p.id",
            "JOIN sys_role r ON r.id = rp.role_id",
            "WHERE r.role_code IN",
            "<foreach collection='roles' item='role' open='(' separator=',' close=')'>#{role}</foreach>",
            "AND p.perm_type = 1 ORDER BY p.sort_no",
            "</script>"
    })
    List<SysPermission> selectMenusByRoles(@Param("roles") List<String> roles);
}
