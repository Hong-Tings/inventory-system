package com.inventory.auth;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.common.result.R;
import com.inventory.system.entity.SysUser;
import com.inventory.system.entity.SysUserRole;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.system.mapper.SysUserRoleMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    public AuthController(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginForm form) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, form.getUsername()));
        if (user == null) return R.fail("用户名或密码错误");
        if (!org.springframework.security.crypto.bcrypt.BCrypt.checkpw(form.getPassword(), user.getPassword())) return R.fail("用户名或密码错误");
        if (user.getStatus() == 0) return R.fail("账号已被禁用");

        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        List<SysUserRole> roles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        List<String> roleNames = roles.stream().map(r -> "role_" + r.getRoleId()).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("position", user.getPosition());
        data.put("roles", roleNames);
        data.put("isAdmin", roleNames.contains("role_1"));
        data.put("token", tokenInfo.getTokenValue());
        return R.ok(data);
    }

    @GetMapping("/userinfo")
    public R<Map<String, Object>> userinfo() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) return R.fail("用户不存在");
        List<SysUserRole> roles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        List<String> roleNames = roles.stream().map(r -> "role_" + r.getRoleId()).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("position", user.getPosition());
        data.put("roles", roleNames);
        data.put("isAdmin", roleNames.contains("role_1"));
        return R.ok(data);
    }

    @PostMapping("/logout")
    public R<Void> logout() { StpUtil.logout(); return R.ok(); }

    static class LoginForm {
        private String username; private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
