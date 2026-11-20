package com.example.demo.service;

import com.example.demo.dto.ReportProjections.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.AnalyticsRepository;
import com.example.demo.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AnalyticsRepository analyticsRepository;
    private final LocationRepository locationRepository;

    public List<NearbyLocationProjection> searchNearbyLocations(double lat, double lng, double radius) {
        if (lat < -89.0 || lat > 89.0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Vĩ độ (lat) phải nằm trong khoảng -89 đến 89.");
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Kinh độ (lng) phải nằm trong khoảng -180 đến 180.");
        }
        if (radius <= 0 || radius > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Bán kính tìm kiếm (radius) phải lớn hơn 0 và tối đa 100 km.");
        }
        return analyticsRepository.searchNearbyLocations(lat, lng, radius);
    }

    public List<AvailableDeviceProjection> getAvailableDevices(Long locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Không tìm thấy địa điểm có ID = " + locationId);
        }
        return analyticsRepository.findAvailableDevicesByLocation(locationId);
    }

    public List<RevenueSummaryProjection> getRevenueSummary(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalDate effectiveEnd = (endDate != null) ? endDate : today;
        LocalDate effectiveStart = (startDate != null) ? startDate : effectiveEnd.minusDays(30);

        if (effectiveEnd.isBefore(effectiveStart)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Ngày kết thúc (endDate) phải lớn hơn hoặc bằng ngày bắt đầu (startDate).");
        }

        LocalDateTime startDateTime = effectiveStart.atStartOfDay();
        LocalDateTime endDateTime = effectiveEnd.atTime(LocalTime.of(23, 59, 59));
        return analyticsRepository.getRevenueSummaryByLocation(startDateTime, endDateTime);
    }

    public List<DailyRevenueProjection> getDailyRevenue(
            LocalDate startDate, LocalDate endDate, Long locationId, String serviceType) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Ngày kết thúc (endDate) phải lớn hơn hoặc bằng ngày bắt đầu (startDate).");
        }

        String normalizedType = null;
        if (serviceType != null && !serviceType.isBlank()) {
            normalizedType = serviceType.trim().toUpperCase(Locale.ROOT);
            if (!"PARKING".equals(normalizedType) && !"CHARGING".equals(normalizedType)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Loại dịch vụ chỉ chấp nhận PARKING hoặc CHARGING.");
            }
        }

        return analyticsRepository.getDailyRevenueFromView(startDate, endDate, locationId, normalizedType);
    }

    public List<DeviceStatusStatsProjection> getDeviceStatusStats() {
        return analyticsRepository.getDeviceStatusStats();
    }

    public List<AdminActivityStatsProjection> getAdminActivityStats() {
        return analyticsRepository.getAdminActivityStats();
    }

    public List<TariffAuditHistoryProjection> getTariffAuditHistory() {
        return analyticsRepository.getTariffAuditHistory();
    }

    public List<LocationAuditHistoryProjection> getLocationAuditHistory() {
        return analyticsRepository.getLocationAuditHistory();
    }

    public List<DeviceAuditHistoryProjection> getDeviceAuditHistory() {
        return analyticsRepository.getDeviceAuditHistory();
    }
}