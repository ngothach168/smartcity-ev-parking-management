package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ev_charger_id")
    private EvCharger evCharger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_spot_id")
    private ParkingSpot parkingSpot;

    @Column(nullable = false, length = 20)
    private String status = "BOOKED";

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt = LocalDateTime.now();

    @Column(name = "expires_at", insertable = false, updatable = false)
    private LocalDateTime expiresAt;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "kwh_consumed", precision = 8, scale = 2)
    private BigDecimal kwhConsumed;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;
}
