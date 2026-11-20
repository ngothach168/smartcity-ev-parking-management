package com.example.demo.controller;

import com.example.demo.dto.ReportProjections.*;
import com.example.demo.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/api/locations/search")
    public ResponseEntity<List<NearbyLocationProjection>> searchNearbyLocations(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radius) {
        return ResponseEntity.ok(analyticsService.searchNearbyLocations(lat, lng, radius));
    }

    @GetMapping("/api/locations/{id}/devices")
    public ResponseEntity<List<AvailableDeviceProjection>> getAvailableDevices(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getAvailableDevices(id));
    }

    @GetMapping("/api/admin/reports/revenue-summary")
    public ResponseEntity<List<RevenueSummaryProjection>> getRevenueSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getRevenueSummary(startDate, endDate));
    }

    @GetMapping("/api/admin/reports/daily-revenue")
    public ResponseEntity<List<DailyRevenueProjection>> getDailyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String serviceType) {
        return ResponseEntity.ok(analyticsService.getDailyRevenue(startDate, endDate, locationId, serviceType));
    }

    @GetMapping("/api/admin/reports/device-status")
    public ResponseEntity<List<DeviceStatusStatsProjection>> getDeviceStatusStats() {
        return ResponseEntity.ok(analyticsService.getDeviceStatusStats());
    }

    @GetMapping("/api/admin/reports/admin-activity")
    public ResponseEntity<List<AdminActivityStatsProjection>> getAdminActivityStats() {
        return ResponseEntity.ok(analyticsService.getAdminActivityStats());
    }

    @GetMapping("/api/admin/reports/audit/tariffs")
    public ResponseEntity<List<TariffAuditHistoryProjection>> getTariffAuditHistory() {
        return ResponseEntity.ok(analyticsService.getTariffAuditHistory());
    }

    @GetMapping("/api/admin/reports/audit/locations")
    public ResponseEntity<List<LocationAuditHistoryProjection>> getLocationAuditHistory() {
        return ResponseEntity.ok(analyticsService.getLocationAuditHistory());
    }

    @GetMapping("/api/admin/reports/audit/devices")
    public ResponseEntity<List<DeviceAuditHistoryProjection>> getDeviceAuditHistory() {
        return ResponseEntity.ok(analyticsService.getDeviceAuditHistory());
    }
}