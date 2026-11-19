package com.example.demo.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // Gọi lên hàm cha của RuntimeException
        this.errorCode = errorCode;
    }
}
