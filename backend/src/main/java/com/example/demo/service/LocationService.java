package com.example.demo.service;

import com.example.demo.repository.LocationDistanceProjection;
import com.example.demo.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private static final double EARTH_DEGREE_KM = 111.045;
    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public List<LocationDistanceProjection> getNearbyAvailableLocations(
            double lat, double lng, double radiusKm, String serviceType) {

        // Tính biên độ khung hình chữ nhật (Bounding Box)
        double latDelta = radiusKm / EARTH_DEGREE_KM;
        double lngDelta = radiusKm / (EARTH_DEGREE_KM * Math.cos(Math.toRadians(lat)));

        return locationRepository.findAvailableNearbyLocations(
                lat, lng,
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta,
                radiusKm,
                serviceType.toUpperCase()
        );
    }
}
