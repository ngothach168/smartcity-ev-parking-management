package com.example.demo.service;

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
        session.setServiceType(serviceType);
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

        // Cắt phần nano-giây để không lọt khe 23:59:59.xxx > 23:59:59
        LocalTime nowTime = LocalTime.now(VN_ZONE).truncatedTo(ChronoUnit.SECONDS);
        Tariff currentTariff = resolveTariffAt(serviceType, nowTime);
        BigDecimal deposit = currentTariff.getBasePrice()
                .multiply(currentTariff.getMultiplier())
                .setScale(2, RoundingMode.HALF_UP);
        session.setDepositAmount(deposit);

        // Trigger AFTER INSERT dưới MySQL tự chuyển thiết bị sang IN_USE
        // và @Generated trên Session tự đọc lại reserved_at, expires_at từ DB
        Session saved = sessionRepository.saveAndFlush(session);
        return sessionMapper.toResponse(saved);
    }

    @Transactional
    public SessionResponse checkIn(Long sessionId, Long actorUserId) {
        Session session = lockAndAuthorizeSession(sessionId, actorUserId);

        if (!"BOOKED".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Chỉ phiên ở trạng thái BOOKED mới có thể check-in (hiện tại: " + session.getStatus() + ").");
        }

        LocalDateTime now = LocalDateTime.now(VN_ZONE).truncatedTo(ChronoUnit.SECONDS);
        if (session.getExpiresAt() != null && !now.isBefore(session.getExpiresAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Phiên đặt chỗ đã quá hạn 15 phút, không thể check-in.");
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
        if (session.getExpiresAt() != null && !now.isBefore(session.getExpiresAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Phiên đặt chỗ đã quá hạn 15 phút và đang chờ hệ thống tự chuyển EXPIRED.");
        }

        // Hủy miễn phí trong 5 phút đầu; sau 5 phút mất tiền cọc (khớp RB14)
        boolean withinFreeWindow = !now.isAfter(session.getReservedAt().plusMinutes(FREE_CANCEL_MINUTES));
        BigDecimal totalAmount = withinFreeWindow
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : session.getDepositAmount();

        session.setStatus("CANCELLED");
        session.setTotalAmount(totalAmount);

        // Trigger AFTER UPDATE dưới MySQL tự giải phóng thiết bị về AVAILABLE
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
                    "Thời gian hoàn tất phải sau thời gian check-in ít nhất 1 giây.");
        }

        BigDecimal kwhConsumed = (req != null) ? req.getKwhConsumed() : null;
        Tariff appliedTariff = resolveTariffAt(session.getServiceType(), session.getStartTime().toLocalTime());
        BigDecimal unitPrice = appliedTariff.getBasePrice().multiply(appliedTariff.getMultiplier());

        BigDecimal calculatedAmount;
        if ("PARKING".equals(session.getServiceType())) {
            if (kwhConsumed != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Phiên đỗ xe (PARKING) không được nhập kwhConsumed.");
            }
            long durationSeconds = Duration.between(session.getStartTime(), endTime).getSeconds();
            long billedHours = Math.max(1L, (durationSeconds + 3599) / 3600);
            calculatedAmount = unitPrice.multiply(BigDecimal.valueOf(billedHours));
            session.setKwhConsumed(null);
        } else {
            if (kwhConsumed == null || kwhConsumed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Phiên sạc điện (CHARGING) bắt buộc phải nhập kwhConsumed lớn hơn 0.");
            }
            calculatedAmount = unitPrice.multiply(kwhConsumed);
            session.setKwhConsumed(kwhConsumed);
        }

        calculatedAmount = calculatedAmount.setScale(2, RoundingMode.HALF_UP);
        // Phí tối thiểu bằng tiền cọc để luôn thỏa RB14 (total_amount >= deposit_amount)
        BigDecimal finalAmount = calculatedAmount.max(session.getDepositAmount());

        session.setStatus("COMPLETED");
        session.setEndTime(endTime);
        session.setTotalAmount(finalAmount);

        // Trigger AFTER UPDATE dưới MySQL tự giải phóng thiết bị về AVAILABLE
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

    private Tariff resolveTariffAt(String serviceType, LocalTime targetTime) {
        List<Tariff> tariffs = tariffRepository.findByServiceTypeOrderByStartTimeAsc(serviceType);
        if (tariffs.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Chưa cấu hình biểu giá cho dịch vụ " + serviceType + ".");
        }

        // Ưu tiên khớp nửa mở [startTime, endTime)
        return tariffs.stream()
                .filter(t -> !targetTime.isBefore(t.getStartTime()) && targetTime.isBefore(t.getEndTime()))
                .findFirst()
                // Fallback cho đúng mốc biên 23:59:59
                .or(() -> tariffs.stream()
                        .filter(t -> !targetTime.isBefore(t.getStartTime()) && !targetTime.isAfter(t.getEndTime()))
                        .findFirst())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy khung giờ biểu giá " + serviceType + " cho thời điểm " + targetTime + "."));
    }
}