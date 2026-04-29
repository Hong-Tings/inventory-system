package com.inventory.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.system.entity.SysRole;
import com.inventory.system.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    public SysRoleService(SysRoleMapper sysRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
    }

    public List<SysRole> list() {
        return sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(SysRole role) {
        sysRoleMapper.insert(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysRole role) {
        sysRoleMapper.updateById(role);
    }
}
