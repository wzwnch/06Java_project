package com.shortlink.common.enums;

import lombok.Getter;

@Getter
public enum LinkStatusEnum {

    NORMAL(0, "正常"),
    RECYCLE(1, "回收站");

    private final Integer code;
    private final String desc;

    LinkStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static LinkStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (LinkStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
