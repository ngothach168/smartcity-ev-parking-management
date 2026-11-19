package com.example.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // System & HTTP Errors
    UNCATEGORIZED_EXCEPTION("SYS_500", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định"),
    VALIDATION_ERROR("REQ_400", HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    INVALID_JSON("REQ_401", HttpStatus.BAD_REQUEST, "Nội dung JSON gửi lên không hợp lệ"),
    SPRING_MVC_ERROR("REQ_ERR", HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),

    // Auth & User Errors
    USER_EXISTED("USER_001", HttpStatus.BAD_REQUEST, "Số điện thoại đã tồn tại trong hệ thống"),
    INVALID_CREDENTIALS("AUTH_401", HttpStatus.UNAUTHORIZED, "Sai số điện thoại hoặc mật khẩu"),
    FORBIDDEN("AUTH_403", HttpStatus.FORBIDDEN, "Không có quyền thực hiện thao tác này"),

    // Business & DB Errors
    RESOURCE_NOT_FOUND("BIZ_404", HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu"),
    BAD_REQUEST("BIZ_400", HttpStatus.BAD_REQUEST, "Vi phạm quy tắc nghiệp vụ"),
    CONFLICT_LOCK("DB_409", HttpStatus.CONFLICT, "Thiết bị đang được giao dịch khác xử lý, vui lòng thử lại"),
    DB_CONSTRAINT_VIOLATION("DB_400", HttpStatus.BAD_REQUEST, "Vi phạm ràng buộc toàn vẹn cơ sở dữ liệu");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}