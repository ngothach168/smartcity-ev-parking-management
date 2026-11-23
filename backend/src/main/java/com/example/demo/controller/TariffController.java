package com.example.demo.controller;

import com.example.demo.dto.TariffPriceUpdateRequest;
import com.example.demo.dto.TariffResponse;
import com.example.demo.dto.TariffScheduleRequest;
import com.example.demo.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;

    @GetMapping("/api/tariffs")
    public ResponseEntity<List<TariffResponse>> getTariffs(
            @RequestParam(required = false) String serviceType) {
        return ResponseEntity.ok(tariffService.getTariffs(serviceType));
    }

    @GetMapping("/api/tariffs/{id}")
    public ResponseEntity<TariffResponse> getTariffById(@PathVariable Long id) {
        return ResponseEntity.ok(tariffService.getTariffById(id));
    }

    @PutMapping("/api/admin/tariffs/schedule")
    public ResponseEntity<List<TariffResponse>> replaceSchedule(
            @RequestHeader("X-Admin-Id") Long adminId,
            @Valid @RequestBody TariffScheduleRequest request) {
        return ResponseEntity.ok(
                tariffService.replaceSchedule(adminId, request.getServiceType(), request.getSlots()));
    }

    @PatchMapping("/api/admin/tariffs/{id}/price")
    public ResponseEntity<TariffResponse> updateTariffPrice(
            @RequestHeader("X-Admin-Id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody TariffPriceUpdateRequest request) {
        return ResponseEntity.ok(tariffService.updateTariffPrice(adminId, id, request));
    }
}