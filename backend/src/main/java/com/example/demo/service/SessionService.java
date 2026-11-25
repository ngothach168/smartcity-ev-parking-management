package com.example.demo.service;

import com.example.demo.dto.SessionCheckInRequest;
import com.example.demo.dto.SessionCompleteRequest;
import com.example.demo.dto.SessionCreateRequest;
import com.example.demo.dto.SessionResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.SessionMapper;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long FREE_CANCEL_MINUTES = 5L;
    private static final long EXPIRE_MINUTES = 15L;
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);
    private static final BigDecimal DEPOSIT_AMOUNT = new BigDecimal("20000.00");

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final EvChargerRepository evChargerRepository;
    private final TariffRepository tariffRepository;
    private final SessionMapper sessionMapper;

    @Transactional
    public SessionResponse createSession(Long actorUserId, SessionCreateRequest req) {
        String serviceType = req.getServiceType().trim().toUpperCase(Locale.ROOT);
        if (!"PARKING".equals(serviceType) && !"CHARGING".equals(serviceType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Loại dịch vụ chỉ chấp nhận PARKING hoặc CHARGING.");
        }

        User user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN,
                        "Tài khoản đặt chỗ không tồn tại."));

        Session session = new Session();
        session.setUser(user);
        session.setStatus("BOOKED");

        if ("PARKING".equals(serviceType)) {
            if (req.getParkingSpotId() == null || req.getEvChargerId() != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Phiên đỗ xe (PARKING) chỉ được truyền parkingSpotId và không được truyền evChargerId.");
            }
            ParkingSpot spot = parkingSpotRepository.findByIdForUpdate(req.getParkingSpotId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Không tìm thấy ô đỗ xe có ID = " + req.getParkingSpotId()));

            if (!"AVAILABLE".equals(spot.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Ô đỗ xe '" + spot.getSpotCode() + "' đã có người đặt hoặc đang bảo trì.");
            }
            session.setParkingSpot(spot);
        } else {
            if (req.getEvChargerId() == null || req.getParkingSpotId() != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Phiên sạc điện (CHARGING) chỉ được truyền evChargerId và không được truyền parkingSpotId.");
            }
            EvCharger charger = evChargerRepository.findByIdForUpdate(req.getEvChargerId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Không tìm thấy trụ sạc có ID = " + req.getEvChargerId()));

            if (!"AVAILABLE".equals(charger.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Trụ sạc '" + charger.getChargerCode() + "' đã có người đặt hoặc đang bảo trì.");
            }
            session.setEvCharger(charger);
        }

        LocalTime nowTime = LocalTime.now(VN_ZONE).truncatedTo(ChronoUnit.SECONDS);
        resolveTariffAt(serviceType, nowTime);
        session.setDepositAmount(DEPOSIT_AMOUNT);

        Session saved = sessionRepository.saveAndFlush(session);
        return sessionMapper.toResponse(saved);
    }

    @Transactional
    public SessionResponse checkIn(Long sessionId, Long actorUserId, SessionCheckInRequest req) {
        Session session = lockAndAuthorizeSession(sessionId, actorUserId);

        if (!"BOOKED".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Chỉ phiên ở trạng thái BOOKED mới có thể xác thực và kích hoạt (hiện tại: " + session.getStatus() + ").");
        }

        LocalDateTime now = LocalDateTime.now(VN_ZONE).truncatedTo(ChronoUnit.SECONDS);
        if (session.getReservedAt() != null && !now.isBefore(session.getReservedAt().plusMinutes(EXPIRE_MINUTES))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Phiên đặt chỗ đã quá hạn 15 phút, không thể kích hoạt.");
        }

        String serviceType = resolveServiceType(session);
        String expectedCode = "PARKING".equals(serviceType)
                ? session.getParkingSpot().getSpotCode()
                : session.getEvCharger().getChargerCode();
        String inputCode = (req != null && req.getDeviceCode() != null) ? req.getDeviceCode().trim() : "";

        if (!expectedCode.equalsIgnoreCase(inputCode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Xác thực thất bại: Mã định vị thiết bị không khớp với vị trí bạn đã đặt.");
        }

        session.setStatus("IN_PROGRESS");
        session.setStartTime(now);

        Session updated = sessionRepository.saveAndFlush(session);
        return sessionMapper.toResponse(updated);
    }

    @Transactional
    public SessionResponse cancelSession(Long sessionId, Long actorUserId) {
        Session session = lockAndAuthorizeSession(sessionId, actorUserId);

        if (!"BOOKED".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Chỉ phiên ở trạng thái BOOKED mới có thể hủy (hiện tại: " + session.getStatus() + ").");
        }

        LocalDateTime now = LocalDateTime.now(VN_ZONE).truncatedTo(ChronoUnit.SECONDS);
        if (session.getReservedAt() != null && !now.isBefore(session.getReservedAt().plusMinutes(EXPIRE_MINUTES))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Phiên đặt chỗ đã quá hạn 15 phút và đang chờ hệ thống tự chuyển EXPIRED.");
        }

        boolean withinFreeWindow = session.getReservedAt() != null
                && !now.isAfter(session.getReservedAt().plusMinutes(FREE_CANCEL_MINUTES));
        BigDecimal totalAmount = withinFreeWindow
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : session.getDepositAmount();

        session.setStatus("CANCELLED");
        session.setTotalAmount(totalAmount);

        Session updated = sessionRepository.saveAndFlush(session);
        return sessionMapper.toResponse(updated);
    }

    @Transactional
    public SessionResponse completeSession(Long sessionId, Long actorUserId, SessionCompleteRequest req) {
        Session session = lockAndAuthorizeSession(sessionId, actorUserId);

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Chỉ phiên đang hoạt động (IN_PROGRESS) mới có thể hoàn tất (hiện tại: " + session.getStatus() + ").");
        }

        LocalDateTime endTime = LocalDateTime.now(VN_ZONE).truncatedTo(ChronoUnit.SECONDS);
        if (!endTime.isAfter(session.getStartTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Thời gian hoàn tất phải sau thời gian kích hoạt ít nhất 1 giây.");
        }

        String serviceType = resolveServiceType(session);
        BigDecimal kwhConsumed = (req != null) ? req.getKwhConsumed() : null;
        if ("PARKING".equals(serviceType)) {
            if (kwhConsumed != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Phiên đỗ xe (PARKING) không được nhập kwhConsumed.");
            }
            session.setKwhConsumed(null);
        } else {
            if (kwhConsumed == null || kwhConsumed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Phiên sạc điện (CHARGING) bắt buộc phải nhập kwhConsumed lớn hơn 0.");
            }
            session.setKwhConsumed(kwhConsumed);
        }

        BigDecimal calculatedAmount = calculateSplitAmount(
                serviceType, session.getStartTime(), endTime, kwhConsumed);

        BigDecimal finalAmount = calculatedAmount.max(session.getDepositAmount());

        session.setStatus("COMPLETED");
        session.setEndTime(endTime);
        session.setTotalAmount(finalAmount);

        Session updated = sessionRepository.saveAndFlush(session);
        return sessionMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSessionById(Long sessionId) {
        Session session = sessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy phiên giao dịch có ID = " + sessionId));
        return sessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getSessionsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Không tìm thấy người dùng có ID = " + userId);
        }
        return sessionRepository.findByUserIdWithDetails(userId).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getAllSessionsForAdmin(String status) {
        List<Session> sessions = (status != null && !status.isBlank())
                ? sessionRepository.findByStatusWithDetails(status.trim().toUpperCase(Locale.ROOT))
                : sessionRepository.findAllWithDetails();

        return sessions.stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    private String resolveServiceType(Session session) {
        if (session.getParkingSpot() != null) {
            return "PARKING";
        }
        if (session.getEvCharger() != null) {
            return "CHARGING";
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST,
                "Phiên giao dịch không gắn với ô đỗ xe hay trụ sạc hợp lệ.");
    }

    private Session lockAndAuthorizeSession(Long sessionId, Long actorUserId) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN,
                        "Tài khoản thao tác không tồn tại."));

        Session session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy phiên giao dịch có ID = " + sessionId));

        boolean isOwner = session.getUser().getId().equals(actor.getId());
        boolean isAdmin = "ADMIN".equals(actor.getRole());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn không có quyền thao tác trên phiên giao dịch của người khác.");
        }
        return session;
    }

    private BigDecimal calculateSplitAmount(
            String serviceType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal kwhConsumed) {

        List<Tariff> tariffs = tariffRepository.findByServiceTypeOrderByStartTimeAsc(serviceType);
        if (tariffs.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Chưa cấu hình biểu giá cho dịch vụ " + serviceType + ".");
        }

        long totalSeconds = Duration.between(startTime, endTime).getSeconds();
        BigDecimal totalSecondsBd = BigDecimal.valueOf(totalSeconds);
        BigDecimal secondsPerHour = BigDecimal.valueOf(3600L);
        BigDecimal totalSum = BigDecimal.ZERO;

        LocalDateTime cursor = startTime;
        int guard = 0;

        while (cursor.isBefore(endTime)) {
            if (++guard > 10000) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Thời gian sử dụng vượt quá giới hạn tính cước.");
            }

            LocalTime cursorTime = cursor.toLocalTime();
            Tariff slot = findTariffInList(tariffs, serviceType, cursorTime);

            LocalDateTime slotBoundary = slot.getEndTime().equals(END_OF_DAY)
                    ? cursor.toLocalDate().plusDays(1).atStartOfDay()
                    : cursor.toLocalDate().atTime(slot.getEndTime());

            LocalDateTime segEnd = slotBoundary.isBefore(endTime) ? slotBoundary : endTime;
            if (!segEnd.isAfter(cursor)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Khung giờ biểu giá " + serviceType + " không liên tục tại thời điểm " + cursorTime + ".");
            }

            long segSeconds = Duration.between(cursor, segEnd).getSeconds();
            BigDecimal unitPrice = slot.getBasePrice().multiply(slot.getMultiplier());

            BigDecimal segmentCost;
            if ("PARKING".equals(serviceType)) {
                segmentCost = BigDecimal.valueOf(segSeconds)
                        .multiply(unitPrice)
                        .divide(secondsPerHour, 10, RoundingMode.HALF_UP);
            } else {
                segmentCost = BigDecimal.valueOf(segSeconds)
                        .multiply(kwhConsumed)
                        .multiply(unitPrice)
                        .divide(totalSecondsBd, 10, RoundingMode.HALF_UP);
            }

            totalSum = totalSum.add(segmentCost);
            cursor = segEnd;
        }

        return totalSum.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);
    }

    private Tariff resolveTariffAt(String serviceType, LocalTime targetTime) {
        List<Tariff> tariffs = tariffRepository.findByServiceTypeOrderByStartTimeAsc(serviceType);
        if (tariffs.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Chưa cấu hình biểu giá cho dịch vụ " + serviceType + ".");
        }
        return findTariffInList(tariffs, serviceType, targetTime);
    }

    private Tariff findTariffInList(List<Tariff> tariffs, String serviceType, LocalTime targetTime) {
        return tariffs.stream()
                .filter(t -> !targetTime.isBefore(t.getStartTime()) && targetTime.isBefore(t.getEndTime()))
                .findFirst()
                .or(() -> tariffs.stream()
                        .filter(t -> !targetTime.isBefore(t.getStartTime()) && !targetTime.isAfter(t.getEndTime()))
                        .findFirst())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy khung giờ biểu giá " + serviceType + " cho thời điểm " + targetTime + "."));
    }
}