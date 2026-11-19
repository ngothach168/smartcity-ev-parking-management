package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            throw new BusinessException(ErrorCode.USER_EXISTED);
        }
        return userRepository.save(user);
    }
}
