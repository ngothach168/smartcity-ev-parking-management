package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SESSIONS")
@Getter
@Setter
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ev_charger_id", updatable = false)
    private EvCharger evCharger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_spot_id", updatable = false)
    private ParkingSpot parkingSpot;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "BOOKED";

    @Generated(event = EventType.INSERT)
    @Column(name = "reserved_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime reservedAt;

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