package com.example.demo.service;

import com.example.demo.dto.TariffPriceUpdateRequest;
import com.example.demo.dto.TariffResponse;
import com.example.demo.dto.TariffSlotRequest;
import com.example.demo.entity.Tariff;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.TariffMapper;
import com.example.demo.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TariffService {

    private static final LocalTime DAY_START = LocalTime.MIDNIGHT;       // 00:00:00
    private static final LocalTime DAY_END   = LocalTime.of(23, 59, 59); // mốc hết ngày

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
                .filter(t -> Boolean.TRUE.equals(t.getActive()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy biểu giá có ID = " + id));
        return tariffMapper.toResponse(tariff);
    }

    @Transactional
    public List<TariffResponse> replaceSchedule(Long adminId, String serviceType, List<TariffSlotRequest> slots) {
        String type = serviceType.trim().toUpperCase(Locale.ROOT);

        List<TariffSlotRequest> sorted = slots.stream()
                .sorted(Comparator.comparing(TariffSlotRequest::getStartTime))
                .toList();
        validateFullDayCoverage(sorted);

        List<TariffResponse> oldSnapshot = tariffRepository.findByServiceTypeOrderByStartTimeAsc(type)
                .stream().map(tariffMapper::toResponse).toList();

        tariffRepository.deactivateByServiceType(type);

        List<Tariff> entities = sorted.stream().map(s -> {
            Tariff t = new Tariff();
            t.setServiceType(type);
            t.setStartTime(s.getStartTime());
            t.setEndTime(s.getEndTime());
            t.setBasePrice(s.getBasePrice());
            t.setMultiplier(s.getMultiplier() != null ? s.getMultiplier() : new BigDecimal("1.00"));
            t.setActive(true);
            return t;
        }).toList();

        List<TariffResponse> newSnapshot = tariffRepository.saveAllAndFlush(entities)
                .stream().map(tariffMapper::toResponse).toList();

        auditLogService.logUpdate(adminId, "TARIFFS", newSnapshot.get(0).getId(), oldSnapshot, newSnapshot);
        return newSnapshot;
    }

    @Transactional
    public TariffResponse updateTariffPrice(Long adminId, Long id, TariffPriceUpdateRequest req) {
        Tariff tariff = tariffRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy biểu giá có ID = " + id));

        TariffResponse oldSnapshot = tariffMapper.toResponse(tariff);

        tariff.setBasePrice(req.getBasePrice());
        if (req.getMultiplier() != null) {
            tariff.setMultiplier(req.getMultiplier());
        }

        Tariff updated = tariffRepository.saveAndFlush(tariff);
        TariffResponse newSnapshot = tariffMapper.toResponse(updated);

        auditLogService.logUpdate(adminId, "TARIFFS", updated.getId(), oldSnapshot, newSnapshot);
        return newSnapshot;
    }

    private void validateFullDayCoverage(List<TariffSlotRequest> sorted) {
        if (!sorted.get(0).getStartTime().equals(DAY_START)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Khung đầu tiên phải bắt đầu từ 00:00:00.");
        }
        for (int i = 0; i < sorted.size(); i++) {
            TariffSlotRequest cur = sorted.get(i);
            if (!cur.getEndTime().isAfter(cur.getStartTime())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Khung bắt đầu " + cur.getStartTime() + " có giờ kết thúc không lớn hơn giờ bắt đầu.");
            }
            if (i > 0) {
                TariffSlotRequest prev = sorted.get(i - 1);
                if (cur.getStartTime().isBefore(prev.getEndTime())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "Khung " + prev.getStartTime() + "-" + prev.getEndTime()
                                    + " bị chồng với khung " + cur.getStartTime() + "-" + cur.getEndTime());
                }
                if (cur.getStartTime().isAfter(prev.getEndTime())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "Bị hở khung giờ từ " + prev.getEndTime() + " đến " + cur.getStartTime());
                }
            }
        }
        if (!sorted.get(sorted.size() - 1).getEndTime().equals(DAY_END)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Khung cuối cùng phải kết thúc lúc 23:59:59.");
        }
    }
}