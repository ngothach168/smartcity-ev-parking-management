package com.example.demo.exception;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        return buildResponse(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(ErrorCode.VALIDATION_ERROR, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return buildResponse(ErrorCode.INVALID_JSON, "Cú pháp JSON hoặc kiểu dữ liệu gửi lên không đúng.");
    }

    @ExceptionHandler({CannotAcquireLockException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleLockConflict(RuntimeException ex) {
        return buildResponse(ErrorCode.CONFLICT_LOCK, extractRootMessage(ex));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, JpaSystemException.class})
    public ResponseEntity<ErrorResponse> handleDatabaseError(RuntimeException ex) {
        String rootMsg = extractRootMessage(ex);
        if (rootMsg.contains("NOWAIT") || rootMsg.contains("3572")) {
            return buildResponse(ErrorCode.CONFLICT_LOCK, rootMsg);
        }
        return buildResponse(ErrorCode.DB_CONSTRAINT_VIOLATION, rootMsg);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnwantedException(Exception ex) {
        // Bắt đúng lỗi chuẩn của Spring MVC (404 sai URL, 405 sai method, 400 thiếu header...) thay vì nuốt thành 500
        if (ex instanceof org.springframework.web.ErrorResponse springEr) {
            HttpStatusCode statusCode = springEr.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(statusCode.value());
            String reason = resolved != null ? resolved.getReasonPhrase() : "HTTP Error";

            ErrorResponse response = ErrorResponse.builder()
                    .status(statusCode.value())
                    .errorCode("HTTP_" + statusCode.value())
                    .message(reason)
                    .detailMessage(ex.getMessage())
                    .dateTime(LocalDateTime.now().toString())
                    .build();
            return ResponseEntity.status(statusCode).body(response);
        }

        return buildResponse(ErrorCode.UNCATEGORIZED_EXCEPTION, extractRootMessage(ex));
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode, String detailMessage) {
        ErrorResponse response = ErrorResponse.builder()
                .status(errorCode.getHttpStatus().value())
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .detailMessage(detailMessage)
                .dateTime(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    private String extractRootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : ex.getMessage();
    }
}