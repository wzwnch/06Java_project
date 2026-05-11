package com.shortlink.utils;

import cn.hutool.crypto.digest.BCrypt;

public final class PasswordUtils {

    private PasswordUtils() {
    }

    public static String encode(String plainPassword) {
        return BCrypt.hashpw(plainPassword);
    }

    public static boolean check(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
