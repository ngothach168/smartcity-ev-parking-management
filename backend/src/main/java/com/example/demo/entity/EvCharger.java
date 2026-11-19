package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "EV_CHARGERS")
@Getter
@Setter
@NoArgsConstructor
public class EvCharger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "charger_code", nullable = false, unique = true, length = 50)
    private String chargerCode;

    @Column(name = "port_type", nullable = false, length = 20)
    private String portType;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "AVAILABLE";
}