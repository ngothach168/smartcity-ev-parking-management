package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.UserCreationRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(UserCreationRequest req) {
        if (userRepository.existsByPhoneNumber(req.getPhoneNumber())) {
            throw new BusinessException(ErrorCode.USER_EXISTED);
        }

        User user = userMapper.toEntity(req);
        user.setFullName(req.getFullName().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole("CUSTOMER");

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest req) {
        User user = userRepository.findByPhoneNumber(req.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return userMapper.toResponse(user);
    }
}