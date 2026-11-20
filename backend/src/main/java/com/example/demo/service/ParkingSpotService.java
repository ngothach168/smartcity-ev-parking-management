package com.example.demo.service;

import com.example.demo.dto.ParkingSpotRequest;
import com.example.demo.dto.ParkingSpotResponse;
import com.example.demo.entity.Location;
import com.example.demo.entity.ParkingSpot;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.ParkingSpotMapper;
import com.example.demo.repository.LocationRepository;
import com.example.demo.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final LocationRepository locationRepository;
    private final ParkingSpotMapper parkingSpotMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ParkingSpotResponse> getAvailableSpots(Long locationId) {
        return parkingSpotRepository.findByLocationIdAndStatus(locationId, "AVAILABLE")
                .stream()
                .map(parkingSpotMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParkingSpotResponse> getSpotsByLocation(Long locationId) {
        return parkingSpotRepository.findByLocationId(locationId)
                .stream()
                .map(parkingSpotMapper::toResponse)
                .toList();
    }

    @Transactional
    public ParkingSpotResponse createSpot(Long adminId, ParkingSpotRequest req) {
        Location location = locationRepository.findById(req.getLocationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy địa điểm có ID = " + req.getLocationId()));

        String code = req.getSpotCode().trim();
        if (parkingSpotRepository.existsBySpotCode(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã ô đỗ '" + code + "' đã tồn tại.");
        }

        ParkingSpot spot = new ParkingSpot();
        spot.setLocation(location);
        spot.setSpotCode(code);
        spot.setVehicleType(req.getVehicleType().trim().toUpperCase(Locale.ROOT));
        spot.setStatus(req.getStatus() != null ? req.getStatus() : "AVAILABLE");

        ParkingSpot saved = parkingSpotRepository.saveAndFlush(spot);
        ParkingSpotResponse response = parkingSpotMapper.toResponse(saved);

        auditLogService.logCreate(adminId, "PARKING_SPOTS", saved.getId(), response);
        return response;
    }

    @Transactional
    public ParkingSpotResponse updateSpot(Long adminId, Long id, ParkingSpotRequest req) {
        ParkingSpot spot = parkingSpotRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy ô đỗ có ID = " + id));

        if ("IN_USE".equals(spot.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Không thể chỉnh sửa ô đỗ đang được sử dụng (IN_USE).");
        }

        if (!spot.getLocation().getId().equals(req.getLocationId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Không được phép thay đổi địa điểm (locationId) của ô đỗ đã tạo.");
        }

        String newCode = req.getSpotCode().trim();
        if (parkingSpotRepository.existsBySpotCodeAndIdNot(newCode, id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Mã ô đỗ '" + newCode + "' đã được sử dụng bởi ô đỗ khác.");
        }

        ParkingSpotResponse oldSnapshot = parkingSpotMapper.toResponse(spot);

        spot.setSpotCode(newCode);
        spot.setVehicleType(req.getVehicleType().trim().toUpperCase(Locale.ROOT));
        if (req.getStatus() != null) {
            spot.setStatus(req.getStatus());
        }

        ParkingSpot updated = parkingSpotRepository.saveAndFlush(spot);
        ParkingSpotResponse newSnapshot = parkingSpotMapper.toResponse(updated);

        auditLogService.logUpdate(adminId, "PARKING_SPOTS", updated.getId(), oldSnapshot, newSnapshot);
        return newSnapshot;
    }
}