package com.shortlink.common.enums;

import lombok.Getter;

@Getter
public enum BizCodeEnum {

    USER_EXIST(1001, "用户名已存在"),
    USERNAME_FORMAT_ERROR(1002, "用户名格式不合法"),
    PASSWORD_FORMAT_ERROR(1003, "密码格式不合法"),
    USER_PHONE_EXIST(1004, "手机号已被绑定"),
    LOGIN_PASSWORD_ERROR(1005, "用户名或密码错误"),
    USER_MAIL_EXIST(1006, "邮箱已被使用"),
    USER_NOT_EXIST(1007, "用户不存在"),
    USER_NOT_LOGIN(1008, "用户未登录"),
    USER_TOKEN_EXPIRE(1009, "Token已过期"),
    USER_TOKEN_INVALID(1010, "Token无效"),
    USER_ACCOUNT_LOCKED(1011, "账号已被锁定"),

    LINK_URL_INVALID(2001, "目标URL格式不合法"),
    LINK_SHORT_CODE_EXIST(2002, "短链接码已存在"),
    LINK_NOT_EXIST(2003, "短链接不存在"),
    LINK_EXPIRED(2004, "短链接已过期"),

    GROUP_NOT_EXIST(3001, "分组不存在"),
    GROUP_EXIST(3002, "分组名称已存在"),
    GROUP_DELETE_ERROR(3003, "分组下存在短链接，无法删除"),

    RECYCLE_RESTORE_ERROR(4001, "短链接码已被占用，无法恢复"),
    RECYCLE_NOT_EXIST(4002, "回收站中不存在该短链接"),

    STATS_TIME_RANGE_ERROR(5001, "查询时间范围超过限制，最大支持30天"),
    STATS_NO_DATA(5002, "暂无统计数据"),

    SYSTEM_ERROR(9999, "系统异常");

    private final Integer code;
    private final String message;

    BizCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
