package com.example.demo.service;

import com.example.demo.dto.AuditLogResponse;
import com.example.demo.entity.AdminAuditLog;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.AdminAuditLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AdminAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void logCreate(Long adminId, String targetTable, Long targetRecordId, Object newPayloadObj) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdmin(userRepository.getReferenceById(adminId));
        log.setActionType("CREATE");
        log.setTargetTable(targetTable);
        log.setTargetRecordId(targetRecordId);
        log.setOldPayload(null);
        log.setNewPayload(toJson(newPayloadObj));
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logUpdate(Long adminId, String targetTable, Long targetRecordId, Object oldPayloadObj, Object newPayloadObj) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdmin(userRepository.getReferenceById(adminId));
        log.setActionType("UPDATE");
        log.setTargetTable(targetTable);
        log.setTargetRecordId(targetRecordId);
        log.setOldPayload(toJson(oldPayloadObj));
        log.setNewPayload(toJson(newPayloadObj));
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllLogs() {
        return auditLogRepository.findAllWithAdminOrderByCreatedAtDesc().stream()
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .adminId(log.getAdmin().getId())
                        .adminName(log.getAdmin().getFullName())
                        .actionType(log.getActionType())
                        .targetTable(log.getTargetTable())
                        .targetRecordId(log.getTargetRecordId())
                        .oldPayload(log.getOldPayload())
                        .newPayload(log.getNewPayload())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Lỗi chuyển đổi payload sang JSON: " + ex.getMessage());
        }
    }
}