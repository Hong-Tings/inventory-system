package com.inventory.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.system.entity.SysOperationLog;
import com.inventory.system.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统日志")
@RestController
@RequestMapping("/api/v1/system/log")
@RequiredArgsConstructor
public class SystemLogController {

    private final SysOperationLogService logService;

    @Operation(summary = "分页查询系统日志")
    @GetMapping("/page")
    public R<PageResult<SysOperationLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operator) {
        return R.ok(PageResult.of(logService.page(new Page<>(page, size), module, action, operator)));
    }
}
