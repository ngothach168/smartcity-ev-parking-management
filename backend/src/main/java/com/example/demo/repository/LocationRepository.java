package com.example.demo.repository;

import com.example.demo.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query(value = """
            SELECT l.id AS id, l.name AS name, l.address AS address,
                   ROUND(ST_Distance_Sphere(POINT(l.longitude, l.latitude), POINT(:lng, :lat)) / 1000, 2) AS distance
            FROM locations l
            WHERE l.latitude BETWEEN :minLat AND :maxLat
              AND l.longitude BETWEEN :minLng AND :maxLng
              AND (
                  (:serviceType = 'PARKING' AND EXISTS (
                      SELECT 1 FROM parking_spots ps 
                      WHERE ps.location_id = l.id AND ps.status = 'AVAILABLE'
                  ))
                  OR
                  (:serviceType = 'CHARGING' AND EXISTS (
                      SELECT 1 FROM ev_chargers ec 
                      WHERE ec.location_id = l.id AND ec.status = 'AVAILABLE'
                  ))
              )
            HAVING distance <= :radiusKm
            ORDER BY distance ASC
            """, nativeQuery = true)
    List<LocationDistanceProjection> findAvailableNearbyLocations(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("radiusKm") double radiusKm,
            @Param("serviceType") String serviceType);
}