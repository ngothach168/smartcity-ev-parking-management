package com.example.demo.controller;

import com.example.demo.dto.TariffRequest;
import com.example.demo.dto.TariffResponse;
import com.example.demo.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/api/admin/tariffs")
    public ResponseEntity<TariffResponse> createTariff(
            @RequestHeader("X-Admin-Id") Long adminId,
            @Valid @RequestBody TariffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tariffService.createTariff(adminId, request));
    }

    @PutMapping("/api/admin/tariffs/{id}")
    public ResponseEntity<TariffResponse> updateTariff(
            @RequestHeader("X-Admin-Id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody TariffRequest request) {
        return ResponseEntity.ok(tariffService.updateTariff(adminId, id, request));
    }
}