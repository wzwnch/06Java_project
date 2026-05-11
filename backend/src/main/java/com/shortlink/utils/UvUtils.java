package com.shortlink.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;

import java.nio.charset.StandardCharsets;

public class UvUtils {

    private static final String UV_COOKIE_NAME = "uv_id";

    private static final int UV_ID_LENGTH = 32;

    private UvUtils() {
    }

    public static String generateUvId(String ip, String userAgent) {
        StringBuilder fingerprint = new StringBuilder();

        if (StrUtil.isNotBlank(ip)) {
            fingerprint.append(ip.trim());
        }

        if (StrUtil.isNotBlank(userAgent)) {
            fingerprint.append("|").append(userAgent.trim());
        }

        if (fingerprint.length() == 0) {
            fingerprint.append(System.currentTimeMillis()).append("|").append(Thread.currentThread().getId());
        }

        return DigestUtil.md5Hex(fingerprint.toString());
    }

    public static String generateUvId(String ip, String userAgent, String acceptLanguage, String acceptEncoding) {
        StringBuilder fingerprint = new StringBuilder();

        if (StrUtil.isNotBlank(ip)) {
            fingerprint.append(ip.trim());
        }

        if (StrUtil.isNotBlank(userAgent)) {
            fingerprint.append("|").append(userAgent.trim());
        }

        if (StrUtil.isNotBlank(acceptLanguage)) {
            fingerprint.append("|").append(acceptLanguage.trim());
        }

        if (StrUtil.isNotBlank(acceptEncoding)) {
            fingerprint.append("|").append(acceptEncoding.trim());
        }

        if (fingerprint.length() == 0) {
            fingerprint.append(System.currentTimeMillis()).append("|").append(Thread.currentThread().getId());
        }

        return DigestUtil.md5Hex(fingerprint.toString());
    }

    public static String generateUvIdFromFingerprint(String... parts) {
        if (parts == null || parts.length == 0) {
            return generateDefaultUvId();
        }

        StringBuilder fingerprint = new StringBuilder();
        for (String part : parts) {
            if (StrUtil.isNotBlank(part)) {
                if (fingerprint.length() > 0) {
                    fingerprint.append("|");
                }
                fingerprint.append(part.trim());
            }
        }

        if (fingerprint.length() == 0) {
            return generateDefaultUvId();
        }

        return DigestUtil.md5Hex(fingerprint.toString());
    }

    private static String generateDefaultUvId() {
        String raw = System.currentTimeMillis() + "|" + Thread.currentThread().getId() + "|" + Math.random();
        return DigestUtil.md5Hex(raw);
    }

    public static String getCookieName() {
        return UV_COOKIE_NAME;
    }

    public static boolean isValidUvId(String uvId) {
        if (StrUtil.isBlank(uvId)) {
            return false;
        }

        String trimmed = uvId.trim();

        if (trimmed.length() != UV_ID_LENGTH) {
            return false;
        }

        for (char c : trimmed.toCharArray()) {
            if (!isHexChar(c)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public static String generateShortUvId(String ip, String userAgent) {
        String fullId = generateUvId(ip, userAgent);
        return fullId.substring(0, 16);
    }

    public static String generateUvIdWithSalt(String ip, String userAgent, String salt) {
        StringBuilder fingerprint = new StringBuilder();

        if (StrUtil.isNotBlank(salt)) {
            fingerprint.append(salt.trim());
        }

        if (StrUtil.isNotBlank(ip)) {
            fingerprint.append("|").append(ip.trim());
        }

        if (StrUtil.isNotBlank(userAgent)) {
            fingerprint.append("|").append(userAgent.trim());
        }

        if (fingerprint.length() == 0) {
            fingerprint.append(System.currentTimeMillis()).append("|").append(Thread.currentThread().getId());
        }

        return DigestUtil.md5Hex(fingerprint.toString());
    }

    public static String generateUipId(String ip) {
        if (StrUtil.isBlank(ip)) {
            return DigestUtil.md5Hex(String.valueOf(System.currentTimeMillis()));
        }

        return DigestUtil.md5Hex(ip.trim());
    }

    public static boolean isSameUv(String uvId1, String uvId2) {
        if (StrUtil.isBlank(uvId1) || StrUtil.isBlank(uvId2)) {
            return false;
        }

        return uvId1.trim().equalsIgnoreCase(uvId2.trim());
    }

    public static String normalizeUvId(String uvId) {
        if (StrUtil.isBlank(uvId)) {
            return null;
        }

        String trimmed = uvId.trim().toLowerCase();

        if (!isValidUvId(trimmed)) {
            return null;
        }

        return trimmed;
    }
}
