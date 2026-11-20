package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionCreateRequest {

    @NotBlank(message = "Loại dịch vụ không được để trống (PARKING hoặc CHARGING)")
    private String serviceType;

    private Long parkingSpotId;

    private Long evChargerId;
}