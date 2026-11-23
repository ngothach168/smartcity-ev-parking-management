-- Tên file: 01_schema_ddl.sql
-- Mô tả: Khởi tạo CSDL gồm 7 bảng vật lý và các ràng buộc toàn vẹn tĩnh (DDL)

DROP DATABASE IF EXISTS ev_parking_db;
CREATE DATABASE ev_parking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ev_parking_db;

SET GLOBAL event_scheduler = ON;

-- Bảng 1: USERS (Tài khoản người dùng)
CREATE TABLE USERS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(10) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',

    -- RB01: Định danh tài khoản (Số điện thoại duy nhất, đúng 10 chữ số)
    CONSTRAINT uq_user_phone UNIQUE (phone_number),
    CONSTRAINT chk_user_phone CHECK (phone_number REGEXP '^[0-9]{10}$'),

    -- RB01: Phân quyền tài khoản
    CONSTRAINT chk_user_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
) ENGINE=InnoDB;

-- Bảng 2: LOCATIONS (Khu vực Bãi đỗ / Trạm sạc)
CREATE TABLE LOCATIONS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(225) NOT NULL,
    location_code VARCHAR(50) NOT NULL,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    address TEXT NOT NULL,

    -- RB02: Mã định danh khu vực duy nhất
    CONSTRAINT uq_location_code UNIQUE (location_code)
) ENGINE=InnoDB;

-- Bảng 3: PARKING_SPOTS (Ô đỗ xe)
CREATE TABLE PARKING_SPOTS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_id BIGINT NOT NULL,
    spot_code VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

    -- RB02: Mã định danh ô đỗ duy nhất
    CONSTRAINT uq_spot_code UNIQUE (spot_code),

    -- RB03: Toàn vẹn cấu trúc (Chặn xóa địa điểm nếu tồn tại ô đỗ)
    CONSTRAINT fk_spot_location FOREIGN KEY (location_id)
        REFERENCES LOCATIONS(id) ON DELETE RESTRICT,

    -- RB04 & RB05: Phạm vi loại phương tiện và trạng thái vật lý
    CONSTRAINT chk_spot_vehicle CHECK (vehicle_type IN ('2_WHEELER', '4_WHEELER')),
    CONSTRAINT chk_spot_status CHECK (status IN ('AVAILABLE', 'IN_USE', 'MAINTENANCE'))
) ENGINE=InnoDB;

-- Bảng 4: EV_CHARGERS (Trụ sạc điện)
CREATE TABLE EV_CHARGERS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_id BIGINT NOT NULL,
    charger_code VARCHAR(50) NOT NULL,
    port_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

    -- RB02: Mã định danh trụ sạc duy nhất
    CONSTRAINT uq_charger_code UNIQUE (charger_code),

    -- RB03: Toàn vẹn cấu trúc (Chặn xóa địa điểm nếu tồn tại trụ sạc)
    CONSTRAINT fk_charger_location FOREIGN KEY (location_id)
        REFERENCES LOCATIONS(id) ON DELETE RESTRICT,

    -- RB04 & RB05: Phạm vi chuẩn cổng sạc và trạng thái vật lý
    CONSTRAINT chk_charger_port CHECK (port_type IN ('CCS2', 'TYPE_2')),
    CONSTRAINT chk_charger_status CHECK (status IN ('AVAILABLE', 'IN_USE', 'MAINTENANCE'))
) ENGINE=InnoDB;

-- Bảng 5: TARIFFS (Bảng giá)
CREATE TABLE TARIFFS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_type VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    base_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    multiplier DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    is_active TINYINT(1) NOT NULL DEFAULT 1,

    -- RB04: Phân loại dịch vụ áp dụng biểu giá
    CONSTRAINT chk_tariff_service CHECK (service_type IN ('PARKING', 'CHARGING')),

    -- RB06: Chu kỳ bảng giá (Cấm lặp lại loại dịch vụ trên cùng một khung giờ)
    CONSTRAINT chk_tariff_time CHECK (end_time > start_time),

    -- RB12: Giới hạn giá trị đơn giá và hệ số nhân không âm
    CONSTRAINT chk_tariff_price CHECK (base_price >= 0.00 AND multiplier >= 0.00)
) ENGINE=InnoDB;

-- Bảng 6: SESSIONS (Phiên giao dịch)
CREATE TABLE SESSIONS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    service_type VARCHAR(20) NOT NULL,
    ev_charger_id BIGINT NULL DEFAULT NULL,
    parking_spot_id BIGINT NULL DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME GENERATED ALWAYS AS (reserved_at + INTERVAL 15 MINUTE) STORED NOT NULL,
    start_time DATETIME NULL DEFAULT NULL,
    end_time DATETIME NULL DEFAULT NULL,
    deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    kwh_consumed DECIMAL(8,2) NULL DEFAULT NULL,
    total_amount DECIMAL(12,2) NULL DEFAULT NULL,

    -- RB07: Toàn vẹn dữ liệu giao dịch (Chặn xóa vật lý khách hàng và thiết bị đã có giao dịch)
    CONSTRAINT fk_session_user FOREIGN KEY (user_id)
        REFERENCES USERS(id) ON DELETE RESTRICT,
    CONSTRAINT fk_session_spot FOREIGN KEY (parking_spot_id)
        REFERENCES PARKING_SPOTS(id) ON DELETE RESTRICT,
    CONSTRAINT fk_session_charger FOREIGN KEY (ev_charger_id)
        REFERENCES EV_CHARGERS(id) ON DELETE RESTRICT,

    -- RB04 & RB10: Phân loại dịch vụ và trạng thái vòng đời phiên
    CONSTRAINT chk_session_service CHECK (service_type IN ('PARKING', 'CHARGING')),
    CONSTRAINT chk_session_status CHECK (
        status IN ('BOOKED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'EXPIRED')
    ),

    -- RB08: Tham chiếu vị trí giao dịch (Phiên đỗ xe chỉ lưu ô đỗ, phiên sạc điện chỉ lưu trụ sạc)
    CONSTRAINT chk_session_xor_location CHECK (
        (service_type = 'PARKING' AND parking_spot_id IS NOT NULL AND ev_charger_id IS NULL) OR
        (service_type = 'CHARGING' AND ev_charger_id IS NOT NULL AND parking_spot_id IS NULL)
    ),

    -- RB11: Toàn vẹn thời gian thực tế theo trạng thái phiên
    CONSTRAINT chk_session_time CHECK (
        (status IN ('BOOKED', 'CANCELLED', 'EXPIRED') AND start_time IS NULL AND end_time IS NULL) OR
        (status = 'IN_PROGRESS' AND start_time IS NOT NULL AND end_time IS NULL) OR
        (status = 'COMPLETED' AND start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)
    ),

    -- RB12: Tiền đóng băng không âm
    CONSTRAINT chk_session_deposit CHECK (deposit_amount >= 0.00),

    -- RB12 & RB13: Ghi nhận và kiểm soát lượng điện tiêu thụ (kWh)
    CONSTRAINT chk_session_kwh CHECK (
        (service_type = 'PARKING' AND kwh_consumed IS NULL) OR
        (service_type = 'CHARGING' AND status != 'COMPLETED' AND kwh_consumed IS NULL) OR
        (service_type = 'CHARGING' AND status = 'COMPLETED' AND kwh_consumed IS NOT NULL AND kwh_consumed > 0.00)
    ),

    -- RB12 & RB14: Đối soát tổng hóa đơn thực thu theo trạng thái đóng/mở phiên
    CONSTRAINT chk_session_total_amount CHECK (
        (status IN ('BOOKED', 'IN_PROGRESS') AND total_amount IS NULL) OR
        (status = 'EXPIRED' AND total_amount IS NOT NULL AND total_amount = deposit_amount) OR
        (status = 'CANCELLED' AND total_amount IS NOT NULL AND (total_amount = 0.00 OR total_amount = deposit_amount)) OR
        (status = 'COMPLETED' AND total_amount IS NOT NULL AND total_amount >= deposit_amount)
    )
) ENGINE=InnoDB;

-- Bảng 7: ADMIN_AUDIT_LOGS (Lịch sử thao tác của Ban quản lý)
CREATE TABLE ADMIN_AUDIT_LOGS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    target_table VARCHAR(50) NOT NULL,
    target_record_id BIGINT NOT NULL,
    old_payload TEXT NULL DEFAULT NULL,
    new_payload TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- RB16: Ràng buộc tài khoản Ban quản lý (Chặn xóa vật lý Admin đã có log)
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id)
        REFERENCES USERS(id) ON DELETE RESTRICT,

    -- RB16: Phạm vi lưu vết hành động và đối tượng bảng cấu hình
    CONSTRAINT chk_audit_action CHECK (action_type IN ('CREATE', 'UPDATE')),
    CONSTRAINT chk_audit_target_table CHECK (
        target_table IN ('LOCATIONS', 'PARKING_SPOTS', 'EV_CHARGERS', 'TARIFFS')
    ),
    CONSTRAINT chk_audit_target_id CHECK (target_record_id > 0),

    -- RB16: Đối soát dữ liệu JSON cũ/mới theo loại hành động
    CONSTRAINT chk_audit_payload CHECK (
        (action_type = 'CREATE' AND old_payload IS NULL AND new_payload IS NOT NULL) OR
        (action_type = 'UPDATE' AND old_payload IS NOT NULL AND new_payload IS NOT NULL)
    )
) ENGINE=InnoDB;

-- Hỗ trợ quét tự động các phiên quá hạn 15 phút (Event) và kiểm tra phiên đang hoạt động (RB15)
CREATE INDEX idx_sessions_status_expires ON SESSIONS (status, expires_at);

-- Hỗ trợ tra cứu và sắp xếp lịch sử thao tác cấu hình của Ban quản lý theo thời gian (RB16)
CREATE INDEX idx_audit_target_created ON ADMIN_AUDIT_LOGS (target_table, action_type, created_at);