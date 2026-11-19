package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tariffs")
@Getter
@Setter
@NoArgsConstructor
public class Tariff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType;

    @Column(name = "start_time", nullable = false)
    private java.time.LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private java.time.LocalTime endTime;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal basePrice;

    @Column(nullable = false, precision = 3, scale = 2)
    private java.math.BigDecimal multiplier = new java.math.BigDecimal("1.00");
}