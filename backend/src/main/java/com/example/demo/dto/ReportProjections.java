package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ReportProjections {

    private ReportProjections() {}

    public interface NearbyLocationProjection {
        Long getLocationId();
        String getLocationCode();
        String getName();
        String getAddress();
        BigDecimal getLatitude();
        BigDecimal getLongitude();
        BigDecimal getDistanceKm();
        Long getSpot2WheelerFree();
        Long getSpot4WheelerFree();
        Long getChargerCcs2Free();
        Long getChargerType2Free();
    }

    public interface AvailableDeviceProjection {
        String getServiceType();
        String getSubType();
        Long getDeviceId();
        String getDeviceCode();
    }

    public interface RevenueSummaryProjection {
        String getLocationCode();
        String getLocationName();
        Long getCompletedCount();
        Long getExpiredCount();
        Long getCancelledWithFeeCount();
        Long getCancelledFreeCount();
        BigDecimal getServiceRevenue();
        BigDecimal getPenaltyRevenue();
        BigDecimal getTotalRevenue();
    }

    public interface DailyRevenueProjection {
        Long getLocationId();
        String getLocationCode();
        String getLocationName();
        String getServiceType();
        LocalDate getRevenueDate();
        Long getTotalSessions();
        BigDecimal getTotalRevenue();
    }

    public interface DeviceStatusStatsProjection {
        Long getLocationId();
        String getLocationCode();
        String getLocationName();
        Long getTotalDevices();
        Long getAvailableChargers();
        Long getInUseChargers();
        Long getMaintenanceChargers();
        Long getAvailableSpots();
        Long getInUseSpots();
        Long getMaintenanceSpots();
    }

    public interface AdminActivityStatsProjection {
        String getAdminId();
        String getAdminName();
        Long getTariffActions();
        Long getInfraActions();
        Long getTotalActions();
    }

    public interface TariffAuditHistoryProjection {
        LocalDateTime getCreatedAt();
        String getAdminName();
        String getServiceType();
        String getTimeSlot();
        String getActionType();
        @JsonRawValue
        String getOldPayload();
        @JsonRawValue
        String getNewPayload();
    }

    public interface LocationAuditHistoryProjection {
        LocalDateTime getCreatedAt();
        String getAdminName();
        String getLocationCode();
        String getLocationName();
        String getActionType();
        @JsonRawValue
        String getOldPayload();
        @JsonRawValue
        String getNewPayload();
    }

    public interface DeviceAuditHistoryProjection {
        LocalDateTime getCreatedAt();
        String getAdminName();
        String getLocationCode();
        String getLocationName();
        String getDeviceTable();
        String getDeviceCode();
        String getSubType();
        String getActionType();
        @JsonRawValue
        String getOldPayload();
        @JsonRawValue
        String getNewPayload();
    }
}