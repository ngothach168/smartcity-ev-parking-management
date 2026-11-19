package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "ADMIN_AUDIT_LOGS")
@Getter
@Setter
@NoArgsConstructor
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false, updatable = false)
    private User admin;

    @Column(name = "action_type", nullable = false, length = 20, updatable = false)
    private String actionType;

    @Column(name = "target_table", nullable = false, length = 50, updatable = false)
    private String targetTable;

    @Column(name = "target_record_id", nullable = false, updatable = false)
    private Long targetRecordId;

    @Column(name = "old_payload", columnDefinition = "TEXT", updatable = false)
    private String oldPayload;

    @Column(name = "new_payload", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String newPayload;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}