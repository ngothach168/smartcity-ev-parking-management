package com.example.demo.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
    private int status;
    private String errorCode;
    private String message;
    private String detailMessage;
    private String dateTime;
}
