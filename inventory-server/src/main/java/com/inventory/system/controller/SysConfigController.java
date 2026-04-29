package com.inventory.system.controller;

import com.inventory.common.result.R;
import com.inventory.system.entity.SysConfig;
import com.inventory.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/api/v1/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "获取全部配置")
    @GetMapping
    public R<List<SysConfig>> list() {
        return R.ok(sysConfigService.list());
    }

    @Operation(summary = "更新配置")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysConfig config) {
        config.setId(id);
        sysConfigService.updateById(config);
        return R.ok();
    }
}
