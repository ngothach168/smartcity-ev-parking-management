package com.example.demo.controller;

import com.example.demo.dto.EvChargerRequest;
import com.example.demo.dto.EvChargerResponse;
import com.example.demo.service.EvChargerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EvChargerController {

    private final EvChargerService evChargerService;

    @GetMapping("/api/locations/{locationId}/ev-chargers")
    public ResponseEntity<List<EvChargerResponse>> getChargersByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(evChargerService.getChargersByLocation(locationId));
    }

    @GetMapping("/api/locations/{locationId}/ev-chargers/available")
    public ResponseEntity<List<EvChargerResponse>> getAvailableChargers(@PathVariable Long locationId) {
        return ResponseEntity.ok(evChargerService.getAvailableChargers(locationId));
    }

    @PostMapping("/api/admin/ev-chargers")
    public ResponseEntity<EvChargerResponse> createCharger(
            @RequestHeader("X-Admin-Id") Long adminId,
            @Valid @RequestBody EvChargerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evChargerService.createCharger(adminId, request));
    }

    @PutMapping("/api/admin/ev-chargers/{id}")
    public ResponseEntity<EvChargerResponse> updateCharger(
            @RequestHeader("X-Admin-Id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody EvChargerRequest request) {
        return ResponseEntity.ok(evChargerService.updateCharger(adminId, id, request));
    }
}