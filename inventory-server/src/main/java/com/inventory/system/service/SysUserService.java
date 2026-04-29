package com.inventory.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.exception.BusinessException;
import com.inventory.system.entity.SysUser;
import com.inventory.system.entity.SysUserRole;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.system.mapper.SysUserRoleMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SysUserService(SysUserMapper sysUserMapper, SysUserRoleMapper userRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public Page<SysUser> page(Page<SysUser> page, String username, String realName, Integer status, String roleType) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(username != null, SysUser::getUsername, username)
                .like(realName != null, SysUser::getRealName, realName)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getId);
        Page<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        for (SysUser user : result.getRecords()) {
            List<SysUserRole> roles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
            user.setRoleNames(roles.stream().map(r -> "role_" + r.getRoleId()).collect(Collectors.toList()));
        }
        // 角色筛选
        if ("admin".equals(roleType)) {
            List<SysUser> filtered = result.getRecords().stream()
                    .filter(u -> u.getRoleNames() != null && u.getRoleNames().contains("role_1"))
                    .collect(Collectors.toList());
            result.setRecords(filtered);
            result.setTotal(filtered.size());
        } else if ("employee".equals(roleType)) {
            List<SysUser> filtered = result.getRecords().stream()
                    .filter(u -> u.getRoleNames() == null || !u.getRoleNames().contains("role_1"))
                    .collect(Collectors.toList());
            result.setRecords(filtered);
            result.setTotal(filtered.size());
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUser(SysUser user) {
        long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) throw new BusinessException("用户名已存在");
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        sysUserMapper.insert(user);
        updateRoles(user.getId(), user.getRoleNames());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        SysUser existing = sysUserMapper.selectById(user.getId());
        if (existing == null) throw new BusinessException("用户不存在");

        // 超级管理员 admin 账号不可降级
        if ("admin".equals(existing.getUsername())) {
            user.setRoleNames(null); // 不修改admin的权限
            user.setStatus(null);    // 不禁用admin
            user.setUsername(null);  // 不改admin用户名
        }

        String newUsername = user.getUsername();
        if (newUsername != null && !newUsername.equals(existing.getUsername())) {
            long c = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, newUsername));
            if (c > 0) throw new BusinessException("用户名已存在");
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        sysUserMapper.updateById(user);

        // 更新角色关联
        if (user.getRoleNames() != null) {
            updateRoles(user.getId(), user.getRoleNames());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        long roleCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id).eq(SysUserRole::getRoleId, 1));
        if ("admin".equals(user.getUsername())) throw new BusinessException("admin账号不可删除");
        sysUserMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    private void updateRoles(Long userId, List<String> roleNames) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleNames != null) {
            for (String roleCode : roleNames) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(Long.parseLong(roleCode.replace("role_", "")));
                userRoleMapper.insert(ur);
            }
        }
    }
}
