package com.lims.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lims.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT r.role_code FROM sys_user_role ur " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0")
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("SELECT DISTINCT p.perm_code FROM sys_user_role ur " +
            "JOIN sys_role_permission rp ON rp.role_id = ur.role_id " +
            "JOIN sys_permission p ON p.id = rp.permission_id " +
            "WHERE ur.user_id = #{userId} AND p.perm_code IS NOT NULL")
    List<String> selectPermCodes(@Param("userId") Long userId);

    /** 拥有某角色的全部用户(用于审批待办通知) */
    @Select("SELECT ur.user_id FROM sys_user_role ur " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "JOIN sys_user u ON u.id = ur.user_id " +
            "WHERE r.role_code = #{roleCode} AND r.status = 1 AND u.status = 1 AND u.deleted = 0")
    List<Long> selectUserIdsByRole(@Param("roleCode") String roleCode);
}
