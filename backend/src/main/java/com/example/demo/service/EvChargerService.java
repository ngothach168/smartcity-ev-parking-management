package com.example.demo.service;

import com.example.demo.dto.EvChargerResponse;
import com.example.demo.repository.EvChargerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvChargerService {

    private final EvChargerRepository evChargerRepository;

    @Transactional(readOnly = true)
    public List<EvChargerResponse> getAvailableChargers(Long locationId) {
        return evChargerRepository.findByLocationIdAndStatus(locationId, "AVAILABLE")
                .stream()
                .map(charger -> EvChargerResponse.builder()
                        .id(charger.getId())
                        .chargerCode(charger.getChargerCode())
                        .portType(charger.getPortType())
                        .build())
                .collect(Collectors.toList());
    }
}
