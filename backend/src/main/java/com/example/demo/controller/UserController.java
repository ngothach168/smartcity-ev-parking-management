package com.example.demo.controller;

import com.example.demo.dto.UserCreationRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper; // Tiêm Mapper vào

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreationRequest request) {
        User user = userMapper.toEntity(request);

        User savedUser = userService.createUser(user);

        UserResponse response = userMapper.toResponse(savedUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
