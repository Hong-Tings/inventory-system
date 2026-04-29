package com.inventory.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.system.entity.SysUser;
import com.inventory.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole("role_1")
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public R<PageResult<SysUser>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String roleType) {
        return R.ok(PageResult.of(userService.page(new Page<>(page, size), username, realName, null, roleType)));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public R<Void> create(@RequestBody SysUser user) {
        userService.saveUser(user);
        return R.ok();
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userService.updateUser(user);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }
}
