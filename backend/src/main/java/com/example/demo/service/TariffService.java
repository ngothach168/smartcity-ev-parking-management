package com.example.demo.service;

import com.example.demo.dto.TariffRequest;
import com.example.demo.dto.TariffResponse;
import com.example.demo.entity.Tariff;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.TariffMapper;
import com.example.demo.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffRepository tariffRepository;
    private final TariffMapper tariffMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<TariffResponse> getTariffs(String serviceType) {
        List<Tariff> tariffs = (serviceType != null && !serviceType.isBlank())
                ? tariffRepository.findByServiceTypeOrderByStartTimeAsc(serviceType.trim().toUpperCase(Locale.ROOT))
                : tariffRepository.findAllByOrderByServiceTypeAscStartTimeAsc();

        return tariffs.stream()
                .map(tariffMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TariffResponse getTariffById(Long id) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy biểu giá có ID = " + id));
        return tariffMapper.toResponse(tariff);
    }

    @Transactional
    public TariffResponse createTariff(Long adminId, TariffRequest req) {
        validateTimeRange(req);

        Tariff tariff = new Tariff();
        tariff.setServiceType(req.getServiceType().trim().toUpperCase(Locale.ROOT));
        tariff.setStartTime(req.getStartTime());
        tariff.setEndTime(req.getEndTime());
        tariff.setBasePrice(req.getBasePrice());
        tariff.setMultiplier(req.getMultiplier() != null ? req.getMultiplier() : new BigDecimal("1.00"));

        Tariff saved = tariffRepository.saveAndFlush(tariff);
        TariffResponse response = tariffMapper.toResponse(saved);

        auditLogService.logCreate(adminId, "TARIFFS", saved.getId(), response);
        return response;
    }

    @Transactional
    public TariffResponse updateTariff(Long adminId, Long id, TariffRequest req) {
        validateTimeRange(req);

        Tariff tariff = tariffRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy biểu giá có ID = " + id));

        TariffResponse oldSnapshot = tariffMapper.toResponse(tariff);

        tariff.setServiceType(req.getServiceType().trim().toUpperCase(Locale.ROOT));
        tariff.setStartTime(req.getStartTime());
        tariff.setEndTime(req.getEndTime());
        tariff.setBasePrice(req.getBasePrice());
        if (req.getMultiplier() != null) {
            tariff.setMultiplier(req.getMultiplier());
        }

        Tariff updated = tariffRepository.saveAndFlush(tariff);
        TariffResponse newSnapshot = tariffMapper.toResponse(updated);

        auditLogService.logUpdate(adminId, "TARIFFS", updated.getId(), oldSnapshot, newSnapshot);
        return newSnapshot;
    }

    private void validateTimeRange(TariffRequest req) {
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Giờ kết thúc phải lớn hơn giờ bắt đầu và nằm trong cùng một ngày.");
        }
    }
}