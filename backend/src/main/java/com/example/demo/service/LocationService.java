package com.example.demo.service;

import com.example.demo.dto.LocationRequest;
import com.example.demo.dto.LocationResponse;
import com.example.demo.entity.Location;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.LocationMapper;
import com.example.demo.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public LocationResponse createLocation(Long adminId, LocationRequest req) {
        String code = req.getLocationCode().trim();
        if (locationRepository.existsByLocationCode(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã địa điểm '" + code + "' đã tồn tại.");
        }

        Location saved = locationRepository.save(locationMapper.toEntity(req));
        LocationResponse response = locationMapper.toResponse(saved);

        auditLogService.logCreate(adminId, "LOCATIONS", saved.getId(), response);
        return response;
    }

    @Transactional
    public LocationResponse updateLocation(Long adminId, Long id, LocationRequest req) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy địa điểm có ID = " + id));

        String newCode = req.getLocationCode().trim();
        if (locationRepository.existsByLocationCodeAndIdNot(newCode, id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã địa điểm '" + newCode + "' đã được sử dụng bởi địa điểm khác.");
        }

        // Chụp snapshot dữ liệu cũ trước khi ghi đè
        LocationResponse oldSnapshot = locationMapper.toResponse(location);

        location.setLocationCode(newCode);
        location.setName(req.getName().trim());
        location.setAddress(req.getAddress().trim());
        location.setLatitude(req.getLatitude());
        location.setLongitude(req.getLongitude());

        Location updated = locationRepository.save(location);
        LocationResponse newSnapshot = locationMapper.toResponse(updated);

        auditLogService.logUpdate(adminId, "LOCATIONS", updated.getId(), oldSnapshot, newSnapshot);
        return newSnapshot;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(locationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse getLocationById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy địa điểm có ID = " + id));
        return locationMapper.toResponse(location);
    }
}