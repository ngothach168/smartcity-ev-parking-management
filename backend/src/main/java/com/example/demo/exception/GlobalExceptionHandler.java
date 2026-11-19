package com.example.demo.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        ErrorResponse response = ErrorResponse.builder()
                .status(errorCode.getHttpStatus().value())
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .detailMessage(ex.getMessage())
                .dateTime(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnwantedException(Exception ex) {
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;

        ErrorResponse response = ErrorResponse.builder()
                .status(errorCode.getHttpStatus().value())
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .detailMessage(ex.getMessage())
                .dateTime(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
}
