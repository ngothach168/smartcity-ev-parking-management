-- Tên file: 01_schema_ddl.sql
-- Mô tả: Khởi tạo CSDL chuẩn theo Lược đồ quan hệ Phase 2 & 13 Ràng buộc hệ thống

DROP DATABASE IF EXISTS ev_parking_db;
CREATE DATABASE ev_parking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ev_parking_db;

SET GLOBAL event_scheduler = ON;

-- Bảng 1: USERS
CREATE TABLE USERS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(10) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('CUSTOMER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    -- Ràng buộc 01
    CONSTRAINT chk_user_phone CHECK (phone_number REGEXP '^[0-9]{10}$')
) ENGINE=InnoDB;

-- Bảng 2: LOCATIONS
CREATE TABLE LOCATIONS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(225) NOT NULL,
    location_code VARCHAR(50) NOT NULL UNIQUE,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    address TEXT NOT NULL
) ENGINE=InnoDB;

-- Bảng 3: PARKING_SPOTS
CREATE TABLE PARKING_SPOTS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_id BIGINT NOT NULL,
    spot_code VARCHAR(50) NOT NULL UNIQUE,
    vehicle_type ENUM('2_WHEELER', '4_WHEELER') NOT NULL,
    status ENUM('AVAILABLE', 'IN_USE', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    -- Ràng buộc 04
    CONSTRAINT fk_parking_location FOREIGN KEY (location_id) REFERENCES LOCATIONS(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Bảng 4: EV_CHARGERS
CREATE TABLE EV_CHARGERS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_id BIGINT NOT NULL,
    charger_code VARCHAR(50) NOT NULL UNIQUE,
    port_type ENUM('CCS2', 'TYPE_2') NOT NULL,
    status ENUM('AVAILABLE', 'IN_USE', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    -- Ràng buộc 04
    CONSTRAINT fk_charger_location FOREIGN KEY (location_id) REFERENCES LOCATIONS(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Bảng 5: TARIFFS
CREATE TABLE TARIFFS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_type ENUM('PARKING', 'CHARGING') NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    base_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    multiplier DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    -- Ràng buộc 11
    CONSTRAINT chk_tariff_time CHECK (end_time > start_time),
    -- Ràng buộc 09
    CONSTRAINT chk_tariff_price CHECK (base_price >= 0.00 AND multiplier >= 0.00)
) ENGINE=InnoDB;

-- Bảng 6: SESSIONS
CREATE TABLE SESSIONS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    service_type ENUM('PARKING', 'CHARGING') NOT NULL,
    ev_charger_id BIGINT NULL DEFAULT NULL,
    parking_spot_id BIGINT NULL DEFAULT NULL,
    status ENUM('BOOKED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'EXPIRED') NOT NULL DEFAULT 'BOOKED',
    reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME GENERATED ALWAYS AS (reserved_at + INTERVAL 15 MINUTE) STORED,
    start_time DATETIME NULL DEFAULT NULL,
    end_time DATETIME NULL DEFAULT NULL,
    deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    kwh_consumed DECIMAL(8,2) NULL DEFAULT NULL,
    total_amount DECIMAL(12,2) NULL DEFAULT NULL,

    -- Ràng buộc 03
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES USERS(id) ON DELETE RESTRICT,
    CONSTRAINT fk_session_charger FOREIGN KEY (ev_charger_id) REFERENCES EV_CHARGERS(id) ON DELETE RESTRICT,
    CONSTRAINT fk_session_spot FOREIGN KEY (parking_spot_id) REFERENCES PARKING_SPOTS(id) ON DELETE RESTRICT,

    -- Ràng buộc 05
    CONSTRAINT chk_session_resource CHECK (
        (service_type = 'CHARGING' AND ev_charger_id IS NOT NULL AND parking_spot_id IS NULL) OR
        (service_type = 'PARKING' AND parking_spot_id IS NOT NULL AND ev_charger_id IS NULL)
    ),
    
    -- Ràng buộc 09
    CONSTRAINT chk_session_non_negative CHECK (
        deposit_amount >= 0.00 AND
        (kwh_consumed IS NULL OR kwh_consumed >= 0.00) AND
        (total_amount IS NULL OR total_amount >= 0.00)
    ),
    
    -- Ràng buộc 10
    CONSTRAINT chk_session_time CHECK (
        (status IN ('BOOKED', 'CANCELLED', 'EXPIRED') AND start_time IS NULL AND end_time IS NULL) OR
        (status = 'IN_PROGRESS' AND start_time IS NOT NULL AND end_time IS NULL) OR
        (status = 'COMPLETED' AND start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)
    ),
    
    -- Nghiệp vụ 06
    CONSTRAINT chk_session_kwh CHECK (
        (service_type = 'PARKING' AND kwh_consumed IS NULL) OR
        (service_type = 'CHARGING' AND (
            (status = 'COMPLETED' AND kwh_consumed IS NOT NULL) OR
            (status != 'COMPLETED' AND kwh_consumed IS NULL)
        ))
    ),
    
    -- Ràng buộc 12
    CONSTRAINT chk_session_total_amount CHECK (
        (status IN ('BOOKED', 'IN_PROGRESS') AND total_amount IS NULL) OR
        (status = 'EXPIRED' AND total_amount = deposit_amount) OR
        (status = 'CANCELLED' AND (total_amount = 0.00 OR total_amount = deposit_amount)) OR
        (status = 'COMPLETED' AND total_amount >= deposit_amount)
    )
) ENGINE=InnoDB;