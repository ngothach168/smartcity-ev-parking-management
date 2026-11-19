package com.example.demo.controller;

import com.example.demo.dto.EvChargerResponse;
import com.example.demo.dto.ParkingSpotResponse;
import com.example.demo.repository.LocationDistanceProjection;
import com.example.demo.service.EvChargerService;
import com.example.demo.service.LocationService;
import com.example.demo.service.ParkingSpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final ParkingSpotService parkingSpotService;
    private final EvChargerService evChargerService;
    private final LocationService locationService;

    @GetMapping("/{locationId}/parking-spots")
    public ResponseEntity<List<ParkingSpotResponse>> getAvailableSpots(@PathVariable Long locationId) {
        return ResponseEntity.ok(parkingSpotService.getAvailableSpots(locationId));
    }

    @GetMapping("/{locationId}/ev-chargers")
    public ResponseEntity<List<EvChargerResponse>> getAvailableChargers(@PathVariable Long locationId) {
        return ResponseEntity.ok(evChargerService.getAvailableChargers(locationId));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<LocationDistanceProjection>> getNearbyLocations(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radius,
            @RequestParam(defaultValue = "PARKING") String type) { // Truyền PARKING hoặc CHARGING

        return ResponseEntity.ok(locationService.getNearbyAvailableLocations(lat, lng, radius, type));
    }
}
