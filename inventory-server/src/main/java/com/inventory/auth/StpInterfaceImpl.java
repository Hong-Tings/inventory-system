package com.inventory.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.inventory.system.entity.SysUserRole;
import com.inventory.system.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysUserRoleMapper userRoleMapper;

    public StpInterfaceImpl(SysUserRoleMapper userRoleMapper) {
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<SysUserRole> roles = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, Long.parseLong(loginId.toString())));
        return roles.stream().map(r -> "role_" + r.getRoleId()).collect(Collectors.toList());
    }
}
