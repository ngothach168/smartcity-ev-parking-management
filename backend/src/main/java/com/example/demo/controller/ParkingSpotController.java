package com.example.demo.controller;

import com.example.demo.dto.ParkingSpotRequest;
import com.example.demo.dto.ParkingSpotResponse;
import com.example.demo.service.ParkingSpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ParkingSpotController {

    private final ParkingSpotService parkingSpotService;

    @GetMapping("/api/locations/{locationId}/parking-spots")
    public ResponseEntity<List<ParkingSpotResponse>> getSpotsByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(parkingSpotService.getSpotsByLocation(locationId));
    }

    @GetMapping("/api/locations/{locationId}/parking-spots/available")
    public ResponseEntity<List<ParkingSpotResponse>> getAvailableSpots(@PathVariable Long locationId) {
        return ResponseEntity.ok(parkingSpotService.getAvailableSpots(locationId));
    }

    @PostMapping("/api/admin/parking-spots")
    public ResponseEntity<ParkingSpotResponse> createSpot(
            @RequestHeader("X-Admin-Id") Long adminId,
            @Valid @RequestBody ParkingSpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parkingSpotService.createSpot(adminId, request));
    }

    @PutMapping("/api/admin/parking-spots/{id}")
    public ResponseEntity<ParkingSpotResponse> updateSpot(
            @RequestHeader("X-Admin-Id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody ParkingSpotRequest request) {
        return ResponseEntity.ok(parkingSpotService.updateSpot(adminId, id, request));
    }
}