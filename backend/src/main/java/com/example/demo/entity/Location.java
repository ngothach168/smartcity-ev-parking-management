package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 225)
    private String name;

    @Column(name = "location_code", nullable = false, unique = true, length = 50)
    private String locationCode;

    @Column(nullable = false, precision = 10, scale = 8)
    private java.math.BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private java.math.BigDecimal longitude;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String address;
}
