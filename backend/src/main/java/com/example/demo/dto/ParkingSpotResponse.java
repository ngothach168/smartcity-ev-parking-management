package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParkingSpotResponse {
    private Long id;
    private Long locationId;
    private String spotCode;
    private String vehicleType;
    private String status;
}