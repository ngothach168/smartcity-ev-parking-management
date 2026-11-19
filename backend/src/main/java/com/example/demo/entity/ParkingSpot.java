package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_spots")
@Getter
@Setter
@NoArgsConstructor
public class ParkingSpot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "spot_code", nullable = false, unique = true, length = 50)
    private String spotCode;

    @Column(name = "vehicle_type", nullable = false, length = 20)
    private String vehicleType;

    @Column(nullable = false, length = 20)
    private String status = "AVAILABLE";
}
