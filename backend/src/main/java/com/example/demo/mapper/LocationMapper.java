package com.example.demo.mapper;

import com.example.demo.dto.LocationRequest;
import com.example.demo.dto.LocationResponse;
import com.example.demo.entity.Location;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public Location toEntity(LocationRequest req) {
        if (req == null) return null;
        Location location = new Location();
        location.setLocationCode(req.getLocationCode().trim());
        location.setName(req.getName().trim());
        location.setAddress(req.getAddress().trim());
        location.setLatitude(req.getLatitude());
        location.setLongitude(req.getLongitude());
        return location;
    }

    public LocationResponse toResponse(Location location) {
        if (location == null) return null;
        return LocationResponse.builder()
                .id(location.getId())
                .locationCode(location.getLocationCode())
                .name(location.getName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }
}