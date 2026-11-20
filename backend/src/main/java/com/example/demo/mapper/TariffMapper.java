package com.example.demo.mapper;

import com.example.demo.dto.TariffResponse;
import com.example.demo.entity.Tariff;
import org.springframework.stereotype.Component;

@Component
public class TariffMapper {

    public TariffResponse toResponse(Tariff tariff) {
        if (tariff == null) return null;
        return TariffResponse.builder()
                .id(tariff.getId())
                .serviceType(tariff.getServiceType())
                .startTime(tariff.getStartTime())
                .endTime(tariff.getEndTime())
                .basePrice(tariff.getBasePrice())
                .multiplier(tariff.getMultiplier())
                .build();
    }
}