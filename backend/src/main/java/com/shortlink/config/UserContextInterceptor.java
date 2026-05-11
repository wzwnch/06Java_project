package com.shortlink.config;

import cn.hutool.core.util.StrUtil;
import com.shortlink.common.enums.BizCodeEnum;
import com.shortlink.common.exception.TokenException;
import com.shortlink.utils.JwtUtils;
import com.shortlink.utils.RedisFallbackHandler;
import com.shortlink.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_BLACKLIST_PREFIX = "link:token:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = extractToken(request);
        
        if (StrUtil.isBlank(token)) {
            throw new TokenException(BizCodeEnum.USER_NOT_LOGIN);
        }

        if (isTokenBlacklisted(token)) {
            throw new TokenException(BizCodeEnum.USER_TOKEN_INVALID.getCode(), "Token已失效，请重新登录");
        }

        if (!JwtUtils.validate(token)) {
            throw new TokenException(BizCodeEnum.USER_TOKEN_INVALID);
        }

        if (JwtUtils.isExpired(token)) {
            throw new TokenException(BizCodeEnum.USER_TOKEN_EXPIRE);
        }

        Long userId = JwtUtils.getUserId(token);
        String username = JwtUtils.getUsername(token);
        if (userId == null || StrUtil.isBlank(username)) {
            throw new TokenException(BizCodeEnum.USER_TOKEN_INVALID);
        }

        UserContext.setUserInfo(userId, username);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StrUtil.isBlank(authHeader)) {
            return null;
        }
        if (authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return authHeader;
    }

    private boolean isTokenBlacklisted(String token) {
        if (!RedisFallbackHandler.isRedisAvailable()) {
            log.warn("Redis不可用，跳过Token黑名单检查");
            return false;
        }

        try {
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
            String value = stringRedisTemplate.opsForValue().get(blacklistKey);
            return StrUtil.isNotBlank(value);
        } catch (Exception e) {
            log.warn("检查Token黑名单失败，降级跳过检查: {}", e.getMessage());
            if (isRedisConnectionError(e)) {
                RedisFallbackHandler.markRedisUnavailable();
            }
            return false;
        }
    }

    private boolean isRedisConnectionError(Exception e) {
        String exceptionName = e.getClass().getName();
        return exceptionName.contains("RedisConnectionFailureException") ||
               exceptionName.contains("RedisConnectionException") ||
               exceptionName.contains("JedisConnectionException") ||
               exceptionName.contains("ConnectionException") ||
               e.getCause() != null && isRedisConnectionError(e.getCause());
    }

    private boolean isRedisConnectionError(Throwable e) {
        if (e == null) {
            return false;
        }
        String exceptionName = e.getClass().getName();
        return exceptionName.contains("RedisConnectionFailureException") ||
               exceptionName.contains("RedisConnectionException") ||
               exceptionName.contains("JedisConnectionException") ||
               exceptionName.contains("ConnectionException") ||
               isRedisConnectionError(e.getCause());
    }
}
