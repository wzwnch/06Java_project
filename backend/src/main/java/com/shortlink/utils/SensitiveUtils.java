package com.shortlink.utils;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

public final class SensitiveUtils {

    private SensitiveUtils() {
    }

    public static String phone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return phone;
        }
        return DesensitizedUtil.mobilePhone(phone);
    }

    public static String email(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        return DesensitizedUtil.email(email);
    }

    public static String password() {
        return "******";
    }

    public static String custom(String str, int prefixLen, int suffixLen) {
        if (StrUtil.isBlank(str)) {
            return str;
        }
        if (str.length() <= prefixLen + suffixLen) {
            return StrUtil.hide(str, 0, str.length());
        }
        return StrUtil.hide(str, prefixLen, str.length() - suffixLen);
    }
}
