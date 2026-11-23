package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionCheckInRequest {

    @NotBlank(message = "Mã định vị ô đỗ hoặc trụ sạc không được để trống")
    private String deviceCode;
}