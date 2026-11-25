-- Tên file: 03_triggers_views.sql
-- Mô tả: Khởi tạo 11 Triggers, 1 Event Scheduler và 1 View thống kê doanh thu

USE ev_parking_db;
SET time_zone = '+07:00';

DROP TRIGGER IF EXISTS trg_sessions_before_insert;
DROP TRIGGER IF EXISTS trg_sessions_before_update;
DROP TRIGGER IF EXISTS trg_sessions_after_insert;
DROP TRIGGER IF EXISTS trg_sessions_after_update;
DROP TRIGGER IF EXISTS trg_sessions_block_delete;
DROP TRIGGER IF EXISTS trg_tariffs_before_insert;
DROP TRIGGER IF EXISTS trg_tariffs_before_update;
DROP TRIGGER IF EXISTS trg_tariffs_block_delete;
DROP TRIGGER IF EXISTS trg_audit_logs_before_insert;
DROP TRIGGER IF EXISTS trg_audit_logs_before_update;
DROP TRIGGER IF EXISTS trg_audit_logs_before_delete;
DROP EVENT IF EXISTS evt_expire_booked_sessions;

DELIMITER $$

-- 1. Trigger BEFORE INSERT trên SESSIONS (RB09: Chống đặt trùng vị trí)
CREATE TRIGGER trg_sessions_before_insert
BEFORE INSERT ON SESSIONS
FOR EACH ROW
BEGIN
    DECLARE v_resource_status VARCHAR(20);

    IF NEW.status IN ('BOOKED', 'IN_PROGRESS') THEN
        IF NEW.parking_spot_id IS NOT NULL THEN
            -- Dùng FOR UPDATE NOWAIT để khóa dòng, chống Race Condition khi có 2 luồng cùng đặt 1 ô đỗ
            SELECT status INTO v_resource_status
            FROM PARKING_SPOTS WHERE id = NEW.parking_spot_id FOR UPDATE NOWAIT;

            IF v_resource_status != 'AVAILABLE' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB09: O do xe nay da co nguoi dat hoac dang bao tri.';
            END IF;
        ELSEIF NEW.ev_charger_id IS NOT NULL THEN
            SELECT status INTO v_resource_status
            FROM EV_CHARGERS WHERE id = NEW.ev_charger_id FOR UPDATE NOWAIT;

            IF v_resource_status != 'AVAILABLE' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB09: Tru sac nay da co nguoi dat hoac dang bao tri.';
            END IF;
        END IF;
    END IF;
END $$

-- 2. Trigger BEFORE UPDATE trên SESSIONS (RB09 & RB10: Chống tráo vị trí đang chạy và Kiểm soát vòng đời phiên)
CREATE TRIGGER trg_sessions_before_update
BEFORE UPDATE ON SESSIONS
FOR EACH ROW
BEGIN
    -- RB10: Cấm mọi hành vi chỉnh sửa nếu phiên đã đóng
    IF OLD.status IN ('COMPLETED', 'CANCELLED', 'EXPIRED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB10: Phien giao dich da dong, khong the chinh sua.';
    END IF;

    -- RB09 & RB10: Cấm UPDATE tráo vị trí thiết bị/dịch vụ giữa chừng làm lọt lưới chống trùng và kẹt trạng thái IN_USE
    IF NOT (OLD.user_id <=> NEW.user_id
            AND OLD.parking_spot_id <=> NEW.parking_spot_id
            AND OLD.ev_charger_id <=> NEW.ev_charger_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB09_10: Khong duoc phep trao doi vi tri thiet bi hoac thong tin dinh danh khi phien da khoi tao.';
    END IF;

    -- RB10: Kiểm soát sơ đồ chuyển trạng thái 1 chiều
    IF OLD.status != NEW.status THEN
        IF OLD.status = 'BOOKED' AND NEW.status NOT IN ('IN_PROGRESS', 'CANCELLED', 'EXPIRED') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB10: Tu BOOKED chi duoc chuyen sang IN_PROGRESS, CANCELLED hoac EXPIRED.';
        END IF;

        IF OLD.status = 'IN_PROGRESS' AND NEW.status != 'COMPLETED' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB10: Tu IN_PROGRESS chi duoc chuyen sang COMPLETED.';
        END IF;
    END IF;
END $$

-- 3. Trigger AFTER INSERT trên SESSIONS (Đồng bộ trạng thái thiết bị vật lý)
CREATE TRIGGER trg_sessions_after_insert
AFTER INSERT ON SESSIONS
FOR EACH ROW
BEGIN
    IF NEW.status IN ('BOOKED', 'IN_PROGRESS') THEN
        IF NEW.parking_spot_id IS NOT NULL THEN
            UPDATE PARKING_SPOTS SET status = 'IN_USE' WHERE id = NEW.parking_spot_id;
        ELSEIF NEW.ev_charger_id IS NOT NULL THEN
            UPDATE EV_CHARGERS SET status = 'IN_USE' WHERE id = NEW.ev_charger_id;
        END IF;
    END IF;
END $$

-- 4. Trigger AFTER UPDATE trên SESSIONS (Đồng bộ trạng thái thiết bị vật lý)
CREATE TRIGGER trg_sessions_after_update
AFTER UPDATE ON SESSIONS
FOR EACH ROW
BEGIN
    -- Trả lại trạng thái AVAILABLE khi kết thúc/hủy phiên
    IF NEW.status IN ('COMPLETED', 'CANCELLED', 'EXPIRED') THEN
        IF NEW.parking_spot_id IS NOT NULL THEN
            UPDATE PARKING_SPOTS SET status = 'AVAILABLE' WHERE id = NEW.parking_spot_id;
        ELSEIF NEW.ev_charger_id IS NOT NULL THEN
            UPDATE EV_CHARGERS SET status = 'AVAILABLE' WHERE id = NEW.ev_charger_id;
        END IF;
    END IF;
END $$

-- 5. Trigger BEFORE DELETE trên SESSIONS (RB17: Ngăn chặn xóa dữ liệu trên bảng SESSIONS)
CREATE TRIGGER trg_sessions_block_delete
BEFORE DELETE ON SESSIONS
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' 
    SET MESSAGE_TEXT = 'ERR_RB17: Khong duoc phep xoa du lieu tren bang SESSIONS.';
END $$

-- 6. Trigger BEFORE INSERT trên TARIFFS (RB06: Chống chồng lấn khung giờ cùng loại dịch vụ)
CREATE TRIGGER trg_tariffs_before_insert
BEFORE INSERT ON TARIFFS
FOR EACH ROW
BEGIN
    DECLARE v_overlap_count INT DEFAULT 0;

    -- Chỉ kiểm tra chồng lấn khi dòng mới được chèn ở trạng thái đang áp dụng
    IF NEW.is_active = 1 THEN
        SELECT COUNT(*) INTO v_overlap_count
        FROM TARIFFS
        WHERE service_type = NEW.service_type
          AND is_active = 1
          AND NEW.start_time < end_time
          AND NEW.end_time > start_time;

        IF v_overlap_count > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB06: Khung gio bieu gia bi chong lan voi khung gio da ton tai.';
        END IF;
    END IF;
END $$

-- 7. Trigger BEFORE UPDATE trên TARIFFS (RB06 & RB15: Chống chồng lấn khung giờ và Khóa bảo vệ cập nhật bảng giá)
CREATE TRIGGER trg_tariffs_before_update
BEFORE UPDATE ON TARIFFS
FOR EACH ROW
BEGIN
    DECLARE v_active_count INT DEFAULT 0;
    DECLARE v_overlap_count INT DEFAULT 0;

    -- RB15: Đếm xem có bất kỳ phiên giao dịch nào đang hoạt động trên toàn hệ thống hay không
    SELECT COUNT(*) INTO v_active_count
    FROM SESSIONS
    WHERE status IN ('BOOKED', 'IN_PROGRESS');

    IF v_active_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB15: Khong the cap nhat bang gia khi dang co phien giao dich hoat dong.';
    END IF;

    -- RB06: Kiểm tra chồng lấn nếu có thay đổi giờ, HOẶC nếu đang bật lại một bộ giá cũ
    IF NEW.is_active = 1
       AND NOT (OLD.is_active = 1
                AND OLD.service_type <=> NEW.service_type
                AND OLD.start_time <=> NEW.start_time
                AND OLD.end_time <=> NEW.end_time) THEN
        SELECT COUNT(*) INTO v_overlap_count
        FROM TARIFFS
        WHERE service_type = NEW.service_type
          AND is_active = 1
          AND id != OLD.id
          AND NEW.start_time < end_time
          AND NEW.end_time > start_time;

        IF v_overlap_count > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB06: Khung gio cap nhat bi chong lan voi khung gio khac.';
        END IF;
    END IF;
END $$

-- 8. Trigger BEFORE DELETE trên TARIFFS (RB18: Ngăn chặn xóa dữ liệu trên bảng TARIFFS)
CREATE TRIGGER trg_tariffs_block_delete
BEFORE DELETE ON TARIFFS
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'ERR_RB18: Khong duoc phep xoa du lieu tren bang TARIFFS.';
END $$

-- 9. Trigger BEFORE INSERT trên ADMIN_AUDIT_LOGS (RB16: Ràng buộc đúng quyền tài khoản ADMIN)
CREATE TRIGGER trg_audit_logs_before_insert
BEFORE INSERT ON ADMIN_AUDIT_LOGS
FOR EACH ROW
BEGIN
    DECLARE v_user_role VARCHAR(20);

    SELECT role INTO v_user_role
    FROM USERS WHERE id = NEW.admin_id;

    IF v_user_role != 'ADMIN' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB16: Chi tai khoan co vai tro ADMIN moi duoc ghi nhan lich su thao tac.';
    END IF;
END $$

-- 10. Trigger BEFORE UPDATE trên ADMIN_AUDIT_LOGS (RB16: Tính bất biến - Cấm chỉnh sửa log)
CREATE TRIGGER trg_audit_logs_before_update
BEFORE UPDATE ON ADMIN_AUDIT_LOGS
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ERR_RB16: Tinh bat bien - Nghiem cam chinh sua lich su thao tac quan tri.';
END $$

-- 11. Trigger BEFORE DELETE trên ADMIN_AUDIT_LOGS (RB16: Ngăn chặn xóa dữ liệu trên bảng ADMIN_AUDIT_LOGS)
CREATE TRIGGER trg_audit_logs_before_delete
BEFORE DELETE ON ADMIN_AUDIT_LOGS
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' 
    SET MESSAGE_TEXT = 'ERR_RB16: Khong duoc phep xoa du lieu tren bang ADMIN_AUDIT_LOGS';
END $$

-- 12. Event Scheduler (Tự động quét và hủy đơn Quá hạn 15 phút)
CREATE EVENT evt_expire_booked_sessions
ON SCHEDULE EVERY 1 MINUTE
DO
BEGIN
    UPDATE SESSIONS
    SET status = 'EXPIRED',
        total_amount = deposit_amount
    WHERE status = 'BOOKED'
      AND reserved_at <= NOW() - INTERVAL 15 MINUTE;
END $$

DELIMITER ;

-- 13. View thống kê doanh thu (Nghiệp vụ xuất báo cáo cho Admin)
CREATE OR REPLACE VIEW vw_location_revenue AS
SELECT 
    l.id AS location_id,
    l.location_code,
    l.name AS location_name,
    'PARKING' AS service_type,
    DATE(COALESCE(s.end_time, s.reserved_at)) AS revenue_date,
    COUNT(s.id) AS total_sessions,
    SUM(s.total_amount) AS total_revenue
FROM SESSIONS s
JOIN PARKING_SPOTS ps ON s.parking_spot_id = ps.id
JOIN LOCATIONS l ON ps.location_id = l.id
WHERE s.status IN ('COMPLETED', 'CANCELLED', 'EXPIRED')
  AND s.total_amount > 0
GROUP BY l.id, l.location_code, l.name, DATE(COALESCE(s.end_time, s.reserved_at))

UNION ALL

SELECT 
    l.id AS location_id,
    l.location_code,
    l.name AS location_name,
    'CHARGING' AS service_type,
    DATE(COALESCE(s.end_time, s.reserved_at)) AS revenue_date,
    COUNT(s.id) AS total_sessions,
    SUM(s.total_amount) AS total_revenue
FROM SESSIONS s
JOIN EV_CHARGERS ec ON s.ev_charger_id = ec.id
JOIN LOCATIONS l ON ec.location_id = l.id
WHERE s.status IN ('COMPLETED', 'CANCELLED', 'EXPIRED')
  AND s.total_amount > 0
GROUP BY l.id, l.location_code, l.name, DATE(COALESCE(s.end_time, s.reserved_at));