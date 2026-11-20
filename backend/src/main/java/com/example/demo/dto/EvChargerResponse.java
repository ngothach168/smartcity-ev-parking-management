package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EvChargerResponse {
    private Long id;
    private Long locationId;
    private String chargerCode;
    private String portType;
    private String status;
}