package com.example.demo.repository;

import com.example.demo.dto.ReportProjections.*;
import com.example.demo.entity.Location;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsRepository extends Repository<Location, Long> {

    @Query(value = """
            WITH NearbyLocations AS (
                SELECT
                    l.id,
                    l.location_code,
                    l.name,
                    l.address,
                    l.latitude,
                    l.longitude,
                    ROUND(6371 * 2 * ASIN(LEAST(1.0, SQRT(
                        POWER(SIN(RADIANS(l.latitude - :lat) / 2), 2) +
                        COS(RADIANS(:lat)) * COS(RADIANS(l.latitude)) *
                        POWER(SIN(RADIANS(l.longitude - :lng) / 2), 2)
                    ))), 2) AS distance_km
                FROM LOCATIONS l
                WHERE l.latitude  BETWEEN :lat - (:radius / 111.0)
                                      AND :lat + (:radius / 111.0)
                  AND l.longitude BETWEEN :lng - (:radius / (111.0 * COS(RADIANS(:lat))))
                                      AND :lng + (:radius / (111.0 * COS(RADIANS(:lat))))
                HAVING distance_km <= :radius
            ),
            AvailableSpots AS (
                SELECT
                    ps.location_id,
                    COUNT(*) AS total_available_spots,
                    COUNT(CASE WHEN ps.vehicle_type = '2_WHEELER' THEN 1 END) AS spot_2_wheeler_free,
                    COUNT(CASE WHEN ps.vehicle_type = '4_WHEELER' THEN 1 END) AS spot_4_wheeler_free
                FROM PARKING_SPOTS ps
                JOIN NearbyLocations nl ON ps.location_id = nl.id
                WHERE ps.status = 'AVAILABLE'
                GROUP BY ps.location_id
            ),
            AvailableChargers AS (
                SELECT
                    ec.location_id,
                    COUNT(*) AS total_available_chargers,
                    COUNT(CASE WHEN ec.port_type = 'CCS2'   THEN 1 END) AS charger_ccs2_free,
                    COUNT(CASE WHEN ec.port_type = 'TYPE_2' THEN 1 END) AS charger_type2_free
                FROM EV_CHARGERS ec
                JOIN NearbyLocations nl ON ec.location_id = nl.id
                WHERE ec.status = 'AVAILABLE'
                GROUP BY ec.location_id
            )
            SELECT
                nl.id AS locationId,
                nl.location_code AS locationCode,
                nl.name AS name,
                nl.address AS address,
                nl.latitude AS latitude,
                nl.longitude AS longitude,
                nl.distance_km AS distanceKm,
                COALESCE(s.spot_2_wheeler_free, 0) AS spot2WheelerFree,
                COALESCE(s.spot_4_wheeler_free, 0) AS spot4WheelerFree,
                COALESCE(c.charger_ccs2_free, 0)   AS chargerCcs2Free,
                COALESCE(c.charger_type2_free, 0)  AS chargerType2Free
            FROM NearbyLocations nl
            LEFT JOIN AvailableSpots    s ON s.location_id = nl.id
            LEFT JOIN AvailableChargers c ON c.location_id = nl.id
            WHERE COALESCE(s.total_available_spots, 0) > 0
               OR COALESCE(c.total_available_chargers, 0) > 0
            ORDER BY nl.distance_km ASC, nl.location_code ASC
            """, nativeQuery = true)
    List<NearbyLocationProjection> searchNearbyLocations(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radius);

    @Query(value = """
            SELECT
                'PARKING' AS serviceType,
                ps.vehicle_type AS subType,
                ps.id AS deviceId,
                ps.spot_code AS deviceCode
            FROM PARKING_SPOTS ps
            WHERE ps.location_id = :locationId
              AND ps.status = 'AVAILABLE'

            UNION ALL

            SELECT
                'CHARGING' AS serviceType,
                ec.port_type AS subType,
                ec.id AS deviceId,
                ec.charger_code AS deviceCode
            FROM EV_CHARGERS ec
            WHERE ec.location_id = :locationId
              AND ec.status = 'AVAILABLE'
            ORDER BY serviceType ASC, subType ASC, deviceCode ASC
            """, nativeQuery = true)
    List<AvailableDeviceProjection> findAvailableDevicesByLocation(@Param("locationId") Long locationId);

    @Query(value = """
            WITH SessionLocations AS (
                SELECT
                    COALESCE(ps.location_id, ec.location_id) AS location_id,
                    s.status,
                    s.total_amount
                FROM SESSIONS s
                LEFT JOIN PARKING_SPOTS ps ON s.parking_spot_id = ps.id
                LEFT JOIN EV_CHARGERS ec   ON s.ev_charger_id = ec.id
                WHERE COALESCE(s.end_time, s.reserved_at) BETWEEN :startDate AND :endDate
                  AND s.status IN ('COMPLETED', 'EXPIRED', 'CANCELLED')
            )
            SELECT
                IF(GROUPING(l.id) = 1, 'TỔNG', MAX(l.location_code)) AS locationCode,
                IF(GROUPING(l.id) = 1, 'Toàn hệ thống', MAX(l.name)) AS locationName,
                COUNT(CASE WHEN sl.status = 'COMPLETED' THEN 1 END) AS completedCount,
                COUNT(CASE WHEN sl.status = 'EXPIRED'   THEN 1 END) AS expiredCount,
                COUNT(CASE WHEN sl.status = 'CANCELLED' AND sl.total_amount > 0 THEN 1 END) AS cancelledWithFeeCount,
                COUNT(CASE WHEN sl.status = 'CANCELLED' AND sl.total_amount = 0 THEN 1 END) AS cancelledFreeCount,
                COALESCE(SUM(CASE WHEN sl.status = 'COMPLETED' THEN sl.total_amount ELSE 0 END), 0) AS serviceRevenue,
                COALESCE(SUM(CASE WHEN sl.status IN ('EXPIRED', 'CANCELLED') THEN sl.total_amount ELSE 0 END), 0) AS penaltyRevenue,
                COALESCE(SUM(sl.total_amount), 0) AS totalRevenue
            FROM LOCATIONS l
            LEFT JOIN SessionLocations sl ON l.id = sl.location_id
            GROUP BY l.id WITH ROLLUP
            ORDER BY GROUPING(l.id) ASC, totalRevenue DESC, locationCode ASC
            """, nativeQuery = true)
    List<RevenueSummaryProjection> getRevenueSummaryByLocation(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            SELECT
                location_id AS locationId,
                location_code AS locationCode,
                location_name AS locationName,
                service_type AS serviceType,
                revenue_date AS revenueDate,
                total_sessions AS totalSessions,
                total_revenue AS totalRevenue
            FROM vw_location_revenue
            WHERE (:startDate IS NULL OR revenue_date >= :startDate)
              AND (:endDate IS NULL OR revenue_date <= :endDate)
              AND (:locationId IS NULL OR location_id = :locationId)
              AND (:serviceType IS NULL OR service_type = :serviceType)
            ORDER BY revenue_date DESC, location_code ASC, service_type ASC
            """, nativeQuery = true)
    List<DailyRevenueProjection> getDailyRevenueFromView(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("locationId") Long locationId,
            @Param("serviceType") String serviceType);

    @Query(value = """
            WITH ChargerStats AS (
                SELECT
                    location_id,
                    COUNT(id) AS total_chargers,
                    COUNT(CASE WHEN status = 'AVAILABLE'   THEN 1 END) AS available_chargers,
                    COUNT(CASE WHEN status = 'IN_USE'      THEN 1 END) AS in_use_chargers,
                    COUNT(CASE WHEN status = 'MAINTENANCE' THEN 1 END) AS maintenance_chargers
                FROM EV_CHARGERS
                GROUP BY location_id
            ),
            ParkingStats AS (
                SELECT
                    location_id,
                    COUNT(id) AS total_spots,
                    COUNT(CASE WHEN status = 'AVAILABLE'   THEN 1 END) AS available_spots,
                    COUNT(CASE WHEN status = 'IN_USE'      THEN 1 END) AS in_use_spots,
                    COUNT(CASE WHEN status = 'MAINTENANCE' THEN 1 END) AS maintenance_spots
                FROM PARKING_SPOTS
                GROUP BY location_id
            )
            SELECT
                l.id AS locationId,
                l.location_code AS locationCode,
                l.name AS locationName,
                COALESCE(c.total_chargers, 0) + COALESCE(p.total_spots, 0) AS totalDevices,
                COALESCE(c.available_chargers, 0) AS availableChargers,
                COALESCE(c.in_use_chargers, 0) AS inUseChargers,
                COALESCE(c.maintenance_chargers, 0) AS maintenanceChargers,
                COALESCE(p.available_spots, 0) AS availableSpots,
                COALESCE(p.in_use_spots, 0) AS inUseSpots,
                COALESCE(p.maintenance_spots, 0) AS maintenanceSpots
            FROM LOCATIONS l
            LEFT JOIN ChargerStats c ON l.id = c.location_id
            LEFT JOIN ParkingStats p ON l.id = p.location_id
            ORDER BY (COALESCE(c.maintenance_chargers, 0) + COALESCE(p.maintenance_spots, 0)) DESC, l.location_code ASC
            """, nativeQuery = true)
    List<DeviceStatusStatsProjection> getDeviceStatusStats();

    @Query(value = """
            SELECT
                IF(GROUPING(u.id) = 1, 'TỔNG', CAST(u.id AS CHAR)) AS adminId,
                IF(GROUPING(u.id) = 1, 'Toàn hệ thống', MAX(u.full_name)) AS adminName,
                COUNT(CASE WHEN al.target_table = 'TARIFFS' THEN 1 END) AS tariffActions,
                COUNT(CASE WHEN al.target_table IN ('LOCATIONS', 'PARKING_SPOTS', 'EV_CHARGERS') THEN 1 END) AS infraActions,
                COUNT(al.id) AS totalActions
            FROM USERS u
            LEFT JOIN ADMIN_AUDIT_LOGS al ON u.id = al.admin_id
            WHERE u.role = 'ADMIN'
            GROUP BY u.id WITH ROLLUP
            ORDER BY GROUPING(u.id) ASC, totalActions DESC, u.id ASC
            """, nativeQuery = true)
    List<AdminActivityStatsProjection> getAdminActivityStats();

    @Query(value = """
            SELECT
                al.created_at AS createdAt,
                u.full_name AS adminName,
                t.service_type AS serviceType,
                CONCAT(t.start_time, ' - ', t.end_time) AS timeSlot,
                al.action_type AS actionType,
                al.old_payload AS oldPayload,
                al.new_payload AS newPayload
            FROM ADMIN_AUDIT_LOGS al
            JOIN USERS u        ON al.admin_id = u.id
            LEFT JOIN TARIFFS t ON al.target_record_id = t.id
            WHERE al.target_table = 'TARIFFS'
            ORDER BY al.created_at DESC, al.id DESC
            """, nativeQuery = true)
    List<TariffAuditHistoryProjection> getTariffAuditHistory();

    @Query(value = """
            SELECT
                al.created_at AS createdAt,
                u.full_name AS adminName,
                l.location_code AS locationCode,
                l.name AS locationName,
                al.action_type AS actionType,
                al.old_payload AS oldPayload,
                al.new_payload AS newPayload
            FROM ADMIN_AUDIT_LOGS al
            JOIN USERS u          ON al.admin_id = u.id
            LEFT JOIN LOCATIONS l ON al.target_record_id = l.id
            WHERE al.target_table = 'LOCATIONS'
            ORDER BY al.created_at DESC, al.id DESC
            """, nativeQuery = true)
    List<LocationAuditHistoryProjection> getLocationAuditHistory();

    @Query(value = """
            SELECT
                al.created_at AS createdAt,
                u.full_name AS adminName,
                l.location_code AS locationCode,
                l.name AS locationName,
                al.target_table AS deviceTable,
                COALESCE(ps.spot_code, ec.charger_code) AS deviceCode,
                COALESCE(ps.vehicle_type, ec.port_type) AS subType,
                al.action_type AS actionType,
                al.old_payload AS oldPayload,
                al.new_payload AS newPayload
            FROM ADMIN_AUDIT_LOGS al
            JOIN USERS u ON al.admin_id = u.id
            LEFT JOIN PARKING_SPOTS ps
                ON al.target_table = 'PARKING_SPOTS' AND al.target_record_id = ps.id
            LEFT JOIN EV_CHARGERS ec
                ON al.target_table = 'EV_CHARGERS'   AND al.target_record_id = ec.id
            LEFT JOIN LOCATIONS l
                ON l.id = COALESCE(ps.location_id, ec.location_id)
            WHERE al.target_table IN ('PARKING_SPOTS', 'EV_CHARGERS')
            ORDER BY al.created_at DESC, al.id DESC
            """, nativeQuery = true)
    List<DeviceAuditHistoryProjection> getDeviceAuditHistory();
}