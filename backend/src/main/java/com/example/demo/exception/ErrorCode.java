package com.example.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // System Errors
    UNCATEGORIZED_EXCEPTION("SYS_500", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định"),

    // Business Errors (Nghiệp vụ EV Parking)
    USER_EXISTED("USER_001", HttpStatus.BAD_REQUEST, "Số điện thoại đã tồn tại trong hệ thống");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
