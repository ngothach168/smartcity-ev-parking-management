package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RoleInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Cho qua preflight request của trình duyệt để không bị lỗi CORS khi nối Frontend
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String adminIdHeader = request.getHeader("X-Admin-Id");
        if (adminIdHeader == null || adminIdHeader.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Thiếu header X-Admin-Id để xác thực quyền quản trị.");
        }

        Long adminId;
        try {
            adminId = Long.parseLong(adminIdHeader.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Header X-Admin-Id phải là số nguyên hợp lệ.");
        }

        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "Tài khoản quản trị không tồn tại."));

        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ tài khoản ADMIN mới có quyền thực hiện thao tác này.");
        }

        return true;
    }
}