package com.example.demo.mapper;

import com.example.demo.dto.SessionResponse;
import com.example.demo.entity.EvCharger;
import com.example.demo.entity.Location;
import com.example.demo.entity.ParkingSpot;
import com.example.demo.entity.Session;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionResponse toResponse(Session session) {
        if (session == null) return null;

        ParkingSpot spot = session.getParkingSpot();
        EvCharger charger = session.getEvCharger();
        Location location = (spot != null) ? spot.getLocation()
                : (charger != null ? charger.getLocation() : null);

        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUser().getId())
                .userFullName(session.getUser().getFullName())
                .serviceType(session.getServiceType())
                .locationId(location != null ? location.getId() : null)
                .locationName(location != null ? location.getName() : null)
                .parkingSpotId(spot != null ? spot.getId() : null)
                .spotCode(spot != null ? spot.getSpotCode() : null)
                .evChargerId(charger != null ? charger.getId() : null)
                .chargerCode(charger != null ? charger.getChargerCode() : null)
                .status(session.getStatus())
                .reservedAt(session.getReservedAt())
                .expiresAt(session.getExpiresAt())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .depositAmount(session.getDepositAmount())
                .kwhConsumed(session.getKwhConsumed())
                .totalAmount(session.getTotalAmount())
                .build();
    }
}