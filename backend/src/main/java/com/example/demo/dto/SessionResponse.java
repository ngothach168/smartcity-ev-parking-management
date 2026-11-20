package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SessionResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private String serviceType;
    private Long locationId;
    private String locationName;
    private Long parkingSpotId;
    private String spotCode;
    private Long evChargerId;
    private String chargerCode;
    private String status;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal depositAmount;
    private BigDecimal kwhConsumed;
    private BigDecimal totalAmount;
}