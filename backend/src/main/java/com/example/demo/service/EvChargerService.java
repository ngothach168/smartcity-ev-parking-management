package com.example.demo.service;

import com.example.demo.dto.EvChargerRequest;
import com.example.demo.dto.EvChargerResponse;
import com.example.demo.entity.EvCharger;
import com.example.demo.entity.Location;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.EvChargerMapper;
import com.example.demo.repository.EvChargerRepository;
import com.example.demo.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EvChargerService {

    private final EvChargerRepository evChargerRepository;
    private final LocationRepository locationRepository;
    private final EvChargerMapper evChargerMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<EvChargerResponse> getAvailableChargers(Long locationId) {
        return evChargerRepository.findByLocationIdAndStatus(locationId, "AVAILABLE")
                .stream()
                .map(evChargerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvChargerResponse> getChargersByLocation(Long locationId) {
        return evChargerRepository.findByLocationId(locationId)
                .stream()
                .map(evChargerMapper::toResponse)
                .toList();
    }

    @Transactional
    public EvChargerResponse createCharger(Long adminId, EvChargerRequest req) {
        Location location = locationRepository.findById(req.getLocationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy địa điểm có ID = " + req.getLocationId()));

        String code = req.getChargerCode().trim();
        if (evChargerRepository.existsByChargerCode(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã trụ sạc '" + code + "' đã tồn tại.");
        }

        EvCharger charger = new EvCharger();
        charger.setLocation(location);
        charger.setChargerCode(code);
        charger.setPortType(req.getPortType().trim().toUpperCase(Locale.ROOT));
        charger.setStatus(req.getStatus() != null ? req.getStatus() : "AVAILABLE");

        EvCharger saved = evChargerRepository.saveAndFlush(charger);
        EvChargerResponse response = evChargerMapper.toResponse(saved);

        auditLogService.logCreate(adminId, "EV_CHARGERS", saved.getId(), response);
        return response;
    }

    @Transactional
    public EvChargerResponse updateCharger(Long adminId, Long id, EvChargerRequest req) {
        EvCharger charger = evChargerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy trụ sạc có ID = " + id));

        if ("IN_USE".equals(charger.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Không thể chỉnh sửa trụ sạc đang được sử dụng (IN_USE).");
        }

        if (!charger.getLocation().getId().equals(req.getLocationId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Không được phép thay đổi địa điểm (locationId) của trụ sạc đã tạo.");
        }

        String newCode = req.getChargerCode().trim();
        if (evChargerRepository.existsByChargerCodeAndIdNot(newCode, id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Mã trụ sạc '" + newCode + "' đã được sử dụng bởi trụ sạc khác.");
        }

        EvChargerResponse oldSnapshot = evChargerMapper.toResponse(charger);

        charger.setChargerCode(newCode);
        charger.setPortType(req.getPortType().trim().toUpperCase(Locale.ROOT));
        if (req.getStatus() != null) {
            charger.setStatus(req.getStatus());
        }

        EvCharger updated = evChargerRepository.saveAndFlush(charger);
        EvChargerResponse newSnapshot = evChargerMapper.toResponse(updated);

        auditLogService.logUpdate(adminId, "EV_CHARGERS", updated.getId(), oldSnapshot, newSnapshot);
        return newSnapshot;
    }
}