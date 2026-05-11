package com.shortlink.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private static final String DEFAULT_SECRET = "shortlink-jwt-secret-key-2024";
    private static final String ISSUER = "shortlink";
    private static final int DEFAULT_EXPIRE_HOURS = 24;

    private static String secret;
    private static int expireHours;

    @Value("${shortlink.jwt.secret:shortlink-jwt-secret-key-2024}")
    public void setSecret(String key) {
        JwtUtils.secret = key;
    }

    @Value("${shortlink.jwt.expire-hours:24}")
    public void setExpireHours(int hours) {
        JwtUtils.expireHours = hours;
    }

    private static JWTSigner getSigner() {
        String key = StrUtil.isBlank(secret) ? DEFAULT_SECRET : secret;
        return JWTSignerUtil.hs256(key.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        int hours = expireHours > 0 ? expireHours : DEFAULT_EXPIRE_HOURS;
        Date expireDate = DateUtil.offsetHour(now, hours);

        return JWT.create()
                .setIssuer(ISSUER)
                .setIssuedAt(now)
                .setExpiresAt(expireDate)
                .setPayload("userId", userId)
                .setPayload("username", username)
                .setSigner(getSigner())
                .sign();
    }

    public static Long getUserId(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            JWT jwt = JWT.of(token);
            Object userId = jwt.getPayload("userId");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
            return Long.parseLong(userId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static String getUsername(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            JWT jwt = JWT.of(token);
            Object username = jwt.getPayload("username");
            return username != null ? username.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean validate(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        try {
            JWT jwt = JWT.of(token);
            if (!jwt.setSigner(getSigner()).verify()) {
                return false;
            }
            JWTValidator validator = JWTValidator.of(jwt);
            validator.validateAlgorithm();
            validator.validateDate(DateUtil.date());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isExpired(String token) {
        if (StrUtil.isBlank(token)) {
            return true;
        }
        try {
            JWT jwt = JWT.of(token);
            Date expiresAt = jwt.getPayloads().getDate("exp");
            return expiresAt != null && expiresAt.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public static Date getExpireDate(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            JWT jwt = JWT.of(token);
            return jwt.getPayloads().getDate("exp");
        } catch (Exception e) {
            return null;
        }
    }
}
