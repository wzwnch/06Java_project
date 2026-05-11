package com.shortlink.common.exception;

import lombok.Getter;

@Getter
public class PermissionException extends RuntimeException {

    private final Integer code;
    private final String message;

    public PermissionException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public PermissionException(String message) {
        super(message);
        this.code = 403;
        this.message = message;
    }
}
