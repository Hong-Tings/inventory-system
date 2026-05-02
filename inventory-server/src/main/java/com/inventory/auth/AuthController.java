package com.inventory.auth;

import cn.dev33.satoken.config.SaLoginConfig;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.common.result.R;
import com.inventory.system.entity.SysUser;
import com.inventory.system.mapper.SysUserMapper;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SysUserMapper userMapper;

    public AuthController(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginForm form) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, form.getUsername()));
        if (user == null) return R.fail("用户名或密码错误");
        if (!BCrypt.checkpw(form.getPassword(), user.getPassword())) return R.fail("用户名或密码错误");
        if (user.getStatus() == 0) return R.fail("账号已被禁用");

        SaLoginConfig loginConfig = new SaLoginConfig().setIsConcurrent(false);
        if (Boolean.TRUE.equals(form.getRememberMe())) {
            loginConfig.setTimeout(60 * 60 * 24 * 30); // 记住我：30天
        }
        StpUtil.login(user.getId(), loginConfig);
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("position", user.getPosition());
        data.put("role", user.getRole());
        data.put("isAdmin", user.getRole() != null && user.getRole() == 1);
        data.put("token", tokenInfo.getTokenValue());
        return R.ok(data);
    }

    @GetMapping("/userinfo")
    public R<Map<String, Object>> userinfo() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) return R.fail("用户不存在");

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("position", user.getPosition());
        data.put("role", user.getRole());
        data.put("isAdmin", user.getRole() != null && user.getRole() == 1);
        return R.ok(data);
    }

    @PostMapping("/logout")
    public R<Void> logout() { StpUtil.logout(); return R.ok(); }

    static class LoginForm {
        private String username; private String password;
        private Boolean rememberMe;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Boolean getRememberMe() { return rememberMe; }
        public void setRememberMe(Boolean rememberMe) { this.rememberMe = rememberMe; }
    }
}
