package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogResponse {
    private Long id;
    private Long adminId;
    private String adminName;
    private String actionType;
    private String targetTable;
    private Long targetRecordId;

    @JsonRawValue
    private String oldPayload;

    @JsonRawValue
    private String newPayload;

    private LocalDateTime createdAt;
}