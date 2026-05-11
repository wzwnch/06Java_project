package com.shortlink.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class EncryptUtils {

    private static final String DEFAULT_KEY = "shortlink-aes-key";

    private static String aesKey;

    @Value("${shortlink.aes.key:shortlink-aes-key}")
    public void setAesKey(String key) {
        EncryptUtils.aesKey = key;
    }

    private static AES getAes() {
        String key = StrUtil.isBlank(aesKey) ? DEFAULT_KEY : aesKey;
        byte[] keyBytes = key.getBytes();
        if (keyBytes.length < 16) {
            byte[] newKey = new byte[16];
            System.arraycopy(keyBytes, 0, newKey, 0, keyBytes.length);
            keyBytes = newKey;
        } else if (keyBytes.length > 16) {
            byte[] newKey = new byte[16];
            System.arraycopy(keyBytes, 0, newKey, 0, 16);
            keyBytes = newKey;
        }
        return SecureUtil.aes(keyBytes);
    }

    public static String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return plainText;
        }
        return getAes().encryptHex(plainText);
    }

    public static String decrypt(String cipherText) {
        if (StrUtil.isBlank(cipherText)) {
            return cipherText;
        }
        try {
            return getAes().decryptStr(cipherText);
        } catch (Exception e) {
            return null;
        }
    }

    public static String encryptToBase64(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return plainText;
        }
        return getAes().encryptBase64(plainText);
    }

    public static String decryptFromBase64(String cipherText) {
        if (StrUtil.isBlank(cipherText)) {
            return cipherText;
        }
        try {
            return getAes().decryptStr(cipherText);
        } catch (Exception e) {
            return null;
        }
    }
}
