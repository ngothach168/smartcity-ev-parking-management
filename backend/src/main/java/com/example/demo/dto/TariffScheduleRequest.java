package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TariffScheduleRequest {

    @NotBlank(message = "Loại dịch vụ không được để trống")
    @Size(max = 20, message = "Loại dịch vụ tối đa 20 ký tự")
    private String serviceType;

    @NotEmpty(message = "Phải có ít nhất một khung giờ")
    @Valid
    private List<TariffSlotRequest> slots;
}