package com.shortlink.common.exception;

import lombok.Getter;

@Getter
public class TokenException extends RuntimeException {

    private final Integer code;
    private final String message;

    public TokenException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public TokenException(com.shortlink.common.enums.BizCodeEnum bizCodeEnum) {
        super(bizCodeEnum.getMessage());
        this.code = bizCodeEnum.getCode();
        this.message = bizCodeEnum.getMessage();
    }
}
