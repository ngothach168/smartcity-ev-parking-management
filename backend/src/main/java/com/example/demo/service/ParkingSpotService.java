package com.example.demo.service;

import com.example.demo.dto.ParkingSpotResponse;
import com.example.demo.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;

    @Transactional(readOnly = true)
    public List<ParkingSpotResponse> getAvailableSpots(Long locationId) {
        return parkingSpotRepository.findByLocationIdAndStatus(locationId, "AVAILABLE")
                .stream()
                .map(spot -> ParkingSpotResponse.builder()
                        .id(spot.getId())
                        .spotCode(spot.getSpotCode())
                        .vehicleType(spot.getVehicleType())
                        .build())
                .collect(Collectors.toList());
    }
}
