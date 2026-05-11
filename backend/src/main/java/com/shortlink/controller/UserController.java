package com.shortlink.controller;

import cn.hutool.core.util.StrUtil;
import com.shortlink.common.result.R;
import com.shortlink.dto.req.UserLoginReqDTO;
import com.shortlink.dto.req.UserRegisterReqDTO;
import com.shortlink.dto.req.UserUpdateReqDTO;
import com.shortlink.dto.resp.UserInfoRespDTO;
import com.shortlink.service.UserService;
import com.shortlink.utils.JwtUtils;
import com.shortlink.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户管理", description = "用户注册、登录、退出、信息管理等接口")
public class UserController {

    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String TOKEN_BLACKLIST_PREFIX = "link:token:blacklist:";

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册接口，注册成功后自动创建默认分组")
    public R<Void> register(@Valid @RequestBody UserRegisterReqDTO request) {
        userService.register(request);
        return R.ok();
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口，返回Token和用户基本信息")
    public R<Map<String, Object>> login(@Valid @RequestBody UserLoginReqDTO request) {
        String token = userService.login(request);
        Long userId = JwtUtils.getUserId(token);
        UserInfoRespDTO userInfo = userService.getUserInfo(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userInfo);

        return R.ok(result);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户退出", description = "用户退出登录，使Token失效")
    public R<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (StrUtil.isNotBlank(token)) {
            if (isTokenBlacklisted(token)) {
                return R.ok();
            }
            userService.logout(token);
        }
        return R.ok();
    }

    @GetMapping("/info")
    @Operation(summary = "查询用户信息", description = "查询当前登录用户信息，敏感字段已脱敏")
    public R<UserInfoRespDTO> getUserInfo() {
        Long userId = UserContext.getUserId();
        UserInfoRespDTO userInfo = userService.getUserInfo(userId);
        return R.ok(userInfo);
    }

    @PutMapping("/info")
    @Operation(summary = "修改用户信息", description = "修改当前登录用户信息，支持修改手机号、邮箱、密码")
    public R<Void> updateUserInfo(@Valid @RequestBody UserUpdateReqDTO updateRequest) {
        Long userId = UserContext.getUserId();
        userService.updateUserInfo(userId, updateRequest);
        return R.ok();
    }

    @GetMapping("/check-username")
    @Operation(summary = "检查用户名", description = "检查用户名是否可用，true表示可用，false表示已存在")
    public R<Boolean> checkUsername(
            @Parameter(description = "用户名", required = true)
            @RequestParam @NotBlank(message = "用户名不能为空") String username) {
        boolean available = userService.isUsernameAvailable(username);
        return R.ok(available);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(TOKEN_HEADER);
        if (StrUtil.isBlank(authHeader)) {
            return null;
        }
        if (authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return authHeader;
    }

    private boolean isTokenBlacklisted(String token) {
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        String value = stringRedisTemplate.opsForValue().get(blacklistKey);
        return StrUtil.isNotBlank(value);
    }
}
