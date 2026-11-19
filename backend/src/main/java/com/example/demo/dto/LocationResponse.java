package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LocationResponse {
    private Long id;
    private String locationCode;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
}