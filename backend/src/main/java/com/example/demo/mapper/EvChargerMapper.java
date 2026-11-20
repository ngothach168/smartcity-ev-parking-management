package com.example.demo.mapper;

import com.example.demo.dto.EvChargerResponse;
import com.example.demo.entity.EvCharger;
import org.springframework.stereotype.Component;

@Component
public class EvChargerMapper {

    public EvChargerResponse toResponse(EvCharger charger) {
        if (charger == null) return null;
        return EvChargerResponse.builder()
                .id(charger.getId())
                .locationId(charger.getLocation().getId())
                .chargerCode(charger.getChargerCode())
                .portType(charger.getPortType())
                .status(charger.getStatus())
                .build();
    }
}