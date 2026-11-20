package com.example.demo.mapper;

import com.example.demo.dto.ParkingSpotResponse;
import com.example.demo.entity.ParkingSpot;
import org.springframework.stereotype.Component;

@Component
public class ParkingSpotMapper {

    public ParkingSpotResponse toResponse(ParkingSpot spot) {
        if (spot == null) return null;
        return ParkingSpotResponse.builder()
                .id(spot.getId())
                .locationId(spot.getLocation().getId())
                .spotCode(spot.getSpotCode())
                .vehicleType(spot.getVehicleType())
                .status(spot.getStatus())
                .build();
    }
}