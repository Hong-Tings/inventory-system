package com.inventory.system.controller;

import com.inventory.common.result.R;
import com.inventory.system.entity.SysRole;
import com.inventory.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/v1/role")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService roleService;

    @Operation(summary = "查询所有角色")
    @GetMapping("/list")
    public R<List<SysRole>> list() {
        return R.ok(roleService.list());
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public R<Void> create(@RequestBody SysRole role) {
        roleService.save(role);
        return R.ok();
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.update(role);
        return R.ok();
    }
}
