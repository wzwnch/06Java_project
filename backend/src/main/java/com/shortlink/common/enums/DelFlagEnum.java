package com.shortlink.common.enums;

import lombok.Getter;

@Getter
public enum DelFlagEnum {

    NORMAL(0, "正常"),
    DELETED(1, "已删除");

    private final Integer code;
    private final String desc;

    DelFlagEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static DelFlagEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DelFlagEnum flagEnum : values()) {
            if (flagEnum.getCode().equals(code)) {
                return flagEnum;
            }
        }
        return null;
    }
}
