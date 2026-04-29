package com.inventory.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.system.entity.SysOperationLog;
import com.inventory.system.mapper.SysOperationLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysOperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    public SysOperationLogService(SysOperationLogMapper sysOperationLogMapper) {
        this.sysOperationLogMapper = sysOperationLogMapper;
    }

    public Page<SysOperationLog> page(Page<SysOperationLog> page, String module, String action, String operator) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<SysOperationLog>()
                .eq(module != null, SysOperationLog::getModule, module)
                .eq(action != null, SysOperationLog::getAction, action)
                .like(operator != null, SysOperationLog::getOperator, operator)
                .orderByDesc(SysOperationLog::getId);
        return sysOperationLogMapper.selectPage(page, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(SysOperationLog log) {
        sysOperationLogMapper.insert(log);
    }
}
