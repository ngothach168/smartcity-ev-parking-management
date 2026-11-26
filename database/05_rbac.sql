-- Tên file: 05_rbac.sql
-- Mô tả: Phân quyền truy cập (RBAC).
-- Chạy file này SAU KHI đã chạy 01, 02, 03, 04 để ứng dụng kết nối được Database.

USE ev_parking_db;

-- 1. Tạo user cho ứng dụng Spring Boot (khớp với application.properties)
CREATE USER IF NOT EXISTS 'ev_app'@'localhost' IDENTIFIED BY 'AppPass123$';

-- Cấp quyền cho ứng dụng: Đọc, Thêm, Sửa (Trigger đã chặn xóa nên không cần cấp quyền Xóa)
GRANT SELECT, INSERT, UPDATE ON ev_parking_db.* TO 'ev_app'@'localhost';
SET DEFAULT ROLE ALL TO 'ev_app'@'localhost';


-- =====================================================================
-- PHẦN DƯỚI ĐÂY CHỈ ĐỂ DEMO TRONG BÁO CÁO MỤC 5.2 (Không ảnh hưởng đến App)
-- =====================================================================

-- 2. Tạo Role Khách hàng (Dùng để demo phân quyền RBAC trong báo cáo)
CREATE ROLE IF NOT EXISTS 'app_customer_role';
GRANT SELECT ON ev_parking_db.LOCATIONS     TO 'app_customer_role';
GRANT SELECT ON ev_parking_db.PARKING_SPOTS TO 'app_customer_role';
GRANT SELECT ON ev_parking_db.EV_CHARGERS   TO 'app_customer_role';
GRANT SELECT ON ev_parking_db.TARIFFS       TO 'app_customer_role';
GRANT SELECT, INSERT, UPDATE ON ev_parking_db.SESSIONS TO 'app_customer_role';
GRANT SELECT, INSERT         ON ev_parking_db.USERS    TO 'app_customer_role';

-- 3. Tạo tài khoản demo cho khách hàng
CREATE USER IF NOT EXISTS 'customer_app'@'localhost' IDENTIFIED BY 'ChangeMe_Customer1';
GRANT 'app_customer_role' TO 'customer_app'@'localhost';
SET DEFAULT ROLE 'app_customer_role' TO 'customer_app'@'localhost';

FLUSH PRIVILEGES;