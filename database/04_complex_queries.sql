-- API 1: Tìm trạm gần nhất có thiết bị trống (GET /api/locations/search)
-- Đầu vào: Vĩ độ (@lat), kinh độ (@lng) hiện tại của khách hàng và bán kính tìm kiếm (@radius tính bằng km).
-- Mục đích: Hiển thị danh sách tổng quan các trạm xung quanh vị trí của khách hàng, kèm theo tổng số lượng ô đỗ và trụ sạc đang trống của từng loại để khách lựa chọn.
-- Kỹ thuật: Bounding Box, công thức Haversine, CTE và COUNT(CASE WHEN).

-- Ví dụ: tọa độ Học viện Công nghệ Bưu chính Viễn thông (97 Man Thiện)
SET @lat = 10.84817665724688;
SET @lng = 106.78663250894164;
SET @radius = 5;

WITH NearbyLocations AS (
    SELECT
        l.id,
        l.location_code,
        l.name,
        l.address,
        ROUND(6371 * 2 * ASIN(LEAST(1.0, SQRT(
            POWER(SIN(RADIANS(l.latitude - @lat) / 2), 2) +
            COS(RADIANS(@lat)) * COS(RADIANS(l.latitude)) *
            POWER(SIN(RADIANS(l.longitude - @lng) / 2), 2)
        ))), 2) AS distance_km
    FROM LOCATIONS l
    WHERE l.latitude  BETWEEN @lat - (@radius / 111.0)
                          AND @lat + (@radius / 111.0)
      AND l.longitude BETWEEN @lng - (@radius / (111.0 * COS(RADIANS(@lat))))
                          AND @lng + (@radius / (111.0 * COS(RADIANS(@lat))))
    HAVING distance_km <= @radius
),
AvailableSpots AS (
    SELECT
        ps.location_id,
        COUNT(*) AS total_available_spots,
        COUNT(CASE WHEN ps.vehicle_type = '2_WHEELER' THEN 1 END) AS spot_2_wheeler_free,
        COUNT(CASE WHEN ps.vehicle_type = '4_WHEELER' THEN 1 END) AS spot_4_wheeler_free
    FROM PARKING_SPOTS ps
    JOIN NearbyLocations nl ON ps.location_id = nl.id
    WHERE ps.status = 'AVAILABLE'
    GROUP BY ps.location_id
),
AvailableChargers AS (
    SELECT
        ec.location_id,
        COUNT(*) AS total_available_chargers,
        COUNT(CASE WHEN ec.port_type = 'CCS2'   THEN 1 END) AS charger_ccs2_free,
        COUNT(CASE WHEN ec.port_type = 'TYPE_2' THEN 1 END) AS charger_type2_free
    FROM EV_CHARGERS ec
    JOIN NearbyLocations nl ON ec.location_id = nl.id
    WHERE ec.status = 'AVAILABLE'
    GROUP BY ec.location_id
)
SELECT
    nl.id AS `ID trạm`,
    nl.location_code AS `Mã trạm`,
    nl.name AS `Tên trạm`,
    nl.address AS `Địa chỉ`,
    nl.distance_km AS `Khoảng cách (km)`,
    COALESCE(s.spot_2_wheeler_free, 0) AS `Ô đỗ xe 2 bánh trống`,
    COALESCE(s.spot_4_wheeler_free, 0) AS `Ô đỗ xe 4 bánh trống`,
    COALESCE(c.charger_ccs2_free, 0)   AS `Trụ sạc CCS2 trống`,
    COALESCE(c.charger_type2_free, 0)  AS `Trụ sạc TYPE 2 trống`
FROM NearbyLocations nl
LEFT JOIN AvailableSpots    s ON s.location_id = nl.id
LEFT JOIN AvailableChargers c ON c.location_id = nl.id
WHERE COALESCE(s.total_available_spots, 0) > 0
   OR COALESCE(c.total_available_chargers, 0) > 0
ORDER BY nl.distance_km ASC, nl.location_code ASC;


-- API 2: Lấy danh sách thiết bị trống tại 1 trạm (GET /api/locations/{id}/devices)
-- Đầu vào: Khóa chính của trạm được chọn (@location_id).
-- Mục đích: Lấy danh sách khóa chính (device_id) và mã hiển thị (device_code) của toàn bộ ô đỗ, trụ sạc đang trống tại trạm đó để khách chọn đặt chỗ.
SET @location_id = 1;

SELECT
    'PARKING' AS `Loại dịch vụ`,
    ps.vehicle_type AS `Phân loại`,
    ps.id AS `ID thiết bị`,
    ps.spot_code AS `Mã thiết bị`
FROM PARKING_SPOTS ps
WHERE ps.location_id = @location_id
  AND ps.status = 'AVAILABLE'

UNION ALL

SELECT
    'CHARGING' AS `Loại dịch vụ`,
    ec.port_type AS `Phân loại`,
    ec.id AS `ID thiết bị`,
    ec.charger_code AS `Mã thiết bị`
FROM EV_CHARGERS ec
WHERE ec.location_id = @location_id
  AND ec.status = 'AVAILABLE'
ORDER BY `Loại dịch vụ`, `Phân loại`, `Mã thiết bị`;


-- Thống kê doanh thu theo ngày, khu vực và LOẠI DỊCH VỤ trong khoảng thời gian từ 01/10/2026 đến 13/10/2026.
-- Đầu vào: Từ ngày (@start_date), đến ngày (@end_date) và loại dịch vụ (@service_type, không bắt buộc).
-- Mục đích: Thống kê số phiên đã ghi nhận tiền (hoàn tất, quá hạn, hủy muộn) và tổng doanh thu theo từng ngày, từng khu vực kèm dòng tổng cộng ở cuối; xem cả 2 loại dịch vụ cùng lúc hoặc xem riêng từng loại.

SET @start_date   = '2026-10-01';
SET @end_date     = '2026-10-13';
SET @service_type = NULL;      -- chỉ xem đỗ xe: SET @service_type = 'PARKING';  chỉ xem sạc điện: SET @service_type = 'CHARGING';

SELECT `Ngày`, `Khu vực`, `Dịch vụ`, `Số phiên`, `Doanh thu`
FROM (
    -- Phần 1: Lấy dữ liệu chi tiết bình thường
    SELECT
        DATE(COALESCE(s.end_time, s.reserved_at)) AS `Ngày`,
        l.name AS `Khu vực`,
        s.service_type AS `Dịch vụ`,
        COUNT(s.id) AS `Số phiên`,
        SUM(s.total_amount) AS `Doanh thu`,
        0 AS sort_order  -- Dùng để đẩy dòng tổng xuống dưới
    FROM SESSIONS s
    LEFT JOIN PARKING_SPOTS ps ON s.parking_spot_id = ps.id
    LEFT JOIN EV_CHARGERS ec   ON s.ev_charger_id = ec.id
    JOIN LOCATIONS l           ON l.id = COALESCE(ps.location_id, ec.location_id)
    WHERE s.total_amount > 0
      AND DATE(COALESCE(s.end_time, s.reserved_at)) BETWEEN @start_date AND @end_date
      AND (@service_type IS NULL OR s.service_type = @service_type)
    GROUP BY DATE(COALESCE(s.end_time, s.reserved_at)), l.id, l.location_code, l.name, s.service_type

    UNION ALL

    -- Phần 2: Tính dòng TỔNG CỘNG
    SELECT
        'TỔNG CỘNG' AS `Ngày`,
        '' AS `Khu vực`,
        '' AS `Dịch vụ`,
        COUNT(s.id) AS `Số phiên`,
        SUM(s.total_amount) AS `Doanh thu`,
        1 AS sort_order
    FROM SESSIONS s
    LEFT JOIN PARKING_SPOTS ps ON s.parking_spot_id = ps.id
    LEFT JOIN EV_CHARGERS ec   ON s.ev_charger_id = ec.id
    JOIN LOCATIONS l           ON l.id = COALESCE(ps.location_id, ec.location_id)
    WHERE s.total_amount > 0
      AND DATE(COALESCE(s.end_time, s.reserved_at)) BETWEEN @start_date AND @end_date
      AND (@service_type IS NULL OR s.service_type = @service_type)
) AS result
ORDER BY sort_order ASC, `Ngày` DESC, `Khu vực` ASC, `Dịch vụ` ASC;

-- Thống kê doanh thu theo từng khu vực trong khoảng thời gian từ 01/10/2026 đến 13/10/2026.
-- Đầu vào: Từ ngày (@start_date), đến ngày (@end_date) và khu vực cần xem (@location_id, không bắt buộc).
-- Mục đích: Thống kê toàn diện số lượng đơn (hoàn tất, quá hạn 15 phút, hủy muộn mất cọc, hủy sớm miễn phí) và phân tách rõ doanh thu dịch vụ thực tế với doanh thu từ phí phạt giữ chỗ trên từng khu vực kèm dòng tổng cộng toàn hệ thống.

SET @start_date  = '2026-10-01 00:00:00';
SET @end_date    = '2026-10-13 23:59:59';
SET @location_id = NULL;      -- xem riêng 1 trạm: SET @location_id = 1;

WITH SessionLocations AS (
    SELECT
        COALESCE(ps.location_id, ec.location_id) AS location_id,
        s.status,
        s.total_amount
    FROM SESSIONS s
    LEFT JOIN PARKING_SPOTS ps ON s.parking_spot_id = ps.id
    LEFT JOIN EV_CHARGERS ec   ON s.ev_charger_id = ec.id
    WHERE COALESCE(s.end_time, s.reserved_at) BETWEEN @start_date AND @end_date
      AND s.status IN ('COMPLETED', 'EXPIRED', 'CANCELLED')
)
SELECT
    IF(GROUPING(l.id) = 1, 'TỔNG', MAX(l.location_code)) AS `Mã trạm`,
    IF(GROUPING(l.id) = 1, NULL, MAX(l.name)) AS `Tên trạm`,
    COUNT(CASE WHEN sl.status = 'COMPLETED' THEN 1 END) AS `Đơn hoàn tất`,
    COUNT(CASE WHEN sl.status = 'EXPIRED'   THEN 1 END) AS `Đơn quá hạn`,
    COUNT(CASE WHEN sl.status = 'CANCELLED' AND sl.total_amount > 0 THEN 1 END) AS `Đơn hủy mất cọc`,
    COUNT(CASE WHEN sl.status = 'CANCELLED' AND sl.total_amount = 0 THEN 1 END) AS `Đơn hủy miễn phí`,
    COALESCE(SUM(CASE WHEN sl.status = 'COMPLETED' THEN sl.total_amount ELSE 0 END), 0) AS `Doanh thu dịch vụ`,
    COALESCE(SUM(CASE WHEN sl.status IN ('EXPIRED', 'CANCELLED') THEN sl.total_amount ELSE 0 END), 0) AS `Doanh thu phí phạt`,
    COALESCE(SUM(sl.total_amount), 0) AS `Tổng doanh thu`
FROM LOCATIONS l
LEFT JOIN SessionLocations sl ON l.id = sl.location_id
WHERE (@location_id IS NULL OR l.id = @location_id)
GROUP BY l.id WITH ROLLUP
ORDER BY GROUPING(l.id) ASC, `Tổng doanh thu` DESC, `Mã trạm` ASC;


-- Thống kê số lượng ô đỗ và trụ sạc theo trạng thái tại từng khu vực
-- Đầu vào: Loại thiết bị (@device_type, không bắt buộc).
-- Mục đích: Thống kê số lượng thiết bị đang trống, đang sử dụng và đang bảo trì tại từng trạm.

SET @device_type = NULL;      -- chỉ xem ô đỗ: SET @device_type = 'PARKING';  chỉ xem trụ sạc: SET @device_type = 'CHARGING';

WITH ChargerStats AS (
    SELECT
        location_id,
        COUNT(id) AS total_chargers,
        COUNT(CASE WHEN status = 'AVAILABLE'   THEN 1 END) AS available_chargers,
        COUNT(CASE WHEN status = 'IN_USE'      THEN 1 END) AS in_use_chargers,
        COUNT(CASE WHEN status = 'MAINTENANCE' THEN 1 END) AS maintenance_chargers
    FROM EV_CHARGERS
    GROUP BY location_id
),
ParkingStats AS (
    SELECT
        location_id,
        COUNT(id) AS total_spots,
        COUNT(CASE WHEN status = 'AVAILABLE'   THEN 1 END) AS available_spots,
        COUNT(CASE WHEN status = 'IN_USE'      THEN 1 END) AS in_use_spots,
        COUNT(CASE WHEN status = 'MAINTENANCE' THEN 1 END) AS maintenance_spots
    FROM PARKING_SPOTS
    GROUP BY location_id
)
SELECT
    l.location_code AS `Mã trạm`,
    l.name AS `Tên trạm`,
    COALESCE(c.total_chargers, 0) + COALESCE(p.total_spots, 0) AS `Tổng thiết bị`,
    COALESCE(c.available_chargers, 0) AS `Trụ sạc trống`,
    COALESCE(c.in_use_chargers, 0) AS `Trụ sạc đang dùng`,
    COALESCE(c.maintenance_chargers, 0) AS `Trụ sạc bảo trì`,
    COALESCE(p.available_spots, 0) AS `Ô đỗ trống`,
    COALESCE(p.in_use_spots, 0) AS `Ô đỗ đang dùng`,
    COALESCE(p.maintenance_spots, 0) AS `Ô đỗ bảo trì`
FROM LOCATIONS l
LEFT JOIN ChargerStats c ON l.id = c.location_id AND (@device_type IS NULL OR @device_type = 'CHARGING')
LEFT JOIN ParkingStats p ON l.id = p.location_id AND (@device_type IS NULL OR @device_type = 'PARKING')
ORDER BY (`Trụ sạc bảo trì` + `Ô đỗ bảo trì`) DESC, `Mã trạm` ASC;


-- Xem lịch sử chỉnh sửa bảng giá (Kiểm toán tài chính)
-- Đầu vào: Không có.
-- Mục đích: Truy vết chính xác quản trị viên nào đã thêm mới hoặc cập nhật bảng giá cùng nội dung đối chiếu trước và sau khi sửa.

SELECT
    al.created_at AS `Thời gian`,
    u.full_name AS `Admin thao tác`,
    t.service_type AS `Loại dịch vụ`,
    CONCAT(t.start_time, ' - ', t.end_time) AS `Khung giờ`,
    al.action_type AS `Hành động`,
    COALESCE(al.old_payload, '---') AS `Giá trị cũ`,
    al.new_payload AS `Giá trị mới`
FROM ADMIN_AUDIT_LOGS al
JOIN USERS u        ON al.admin_id = u.id
LEFT JOIN TARIFFS t ON al.target_record_id = t.id
WHERE al.target_table = 'TARIFFS'
ORDER BY al.created_at DESC, al.id DESC;


-- Thống kê tần suất thao tác của từng Admin (Tổng quan nhân sự)
-- Đầu vào: Không có.
-- Mục đích: Giúp Ban quản trị nắm được số lượng cấu hình, chỉnh sửa của từng quản trị viên.

SELECT
    IF(GROUPING(u.id), 'TỔNG', u.id) AS `Mã Admin`,
    IF(GROUPING(u.id), 'Toàn hệ thống', MAX(u.full_name)) AS `Họ tên Admin`,
    COUNT(CASE WHEN al.target_table = 'TARIFFS' THEN 1 END) AS `Thao tác bảng giá`,
    COUNT(CASE WHEN al.target_table IN ('LOCATIONS', 'PARKING_SPOTS', 'EV_CHARGERS') THEN 1 END) AS `Thao tác hạ tầng`,
    COUNT(al.id) AS `Tổng thao tác`
FROM USERS u
LEFT JOIN ADMIN_AUDIT_LOGS al ON u.id = al.admin_id
WHERE u.role = 'ADMIN'
GROUP BY u.id WITH ROLLUP
ORDER BY GROUPING(u.id) ASC, `Tổng thao tác` DESC, u.id ASC;


-- Xem lịch sử thêm mới và chỉnh sửa khu vực (Kiểm toán mặt bằng trạm)
-- Đầu vào: Không có.
-- Mục đích: Truy xuất lịch sử khởi tạo hoặc cập nhật thông tin các khu vực trạm sạc, bãi đỗ để đối soát hạ tầng mặt bằng.

SELECT
    al.created_at AS `Thời gian`,
    u.full_name AS `Admin thao tác`,
    l.location_code AS `Mã trạm`,
    l.name AS `Tên trạm`,
    al.action_type AS `Hành động`,
    COALESCE(al.old_payload, '---') AS `Giá trị cũ`,
    al.new_payload AS `Giá trị mới`
FROM ADMIN_AUDIT_LOGS al
JOIN USERS u          ON al.admin_id = u.id
LEFT JOIN LOCATIONS l ON al.target_record_id = l.id
WHERE al.target_table = 'LOCATIONS'
ORDER BY al.created_at DESC, al.id DESC;


-- Xem lịch sử thêm mới và chỉnh sửa thiết bị (Ô đỗ xe & Trụ sạc)
-- Đầu vào: Không có.
-- Mục đích: Xem quản trị viên nào đã thêm mới hoặc cập nhật ô đỗ xe, trụ sạc, kèm mã thiết bị, loại thiết bị và trạm quản lý .

SELECT
    al.created_at AS `Thời gian`,
    u.full_name AS `Admin thao tác`,
    COALESCE(l.location_code, '---') AS `Mã trạm`,
    COALESCE(l.name, '---') AS `Tên trạm`,
    al.target_table AS `Loại thiết bị`,
    COALESCE(ps.spot_code, ec.charger_code, '---') AS `Mã thiết bị`,
    COALESCE(ps.vehicle_type, ec.port_type, '---') AS `Phân loại`,
    al.action_type AS `Hành động`,
    COALESCE(al.old_payload, '---') AS `Giá trị cũ`,
    al.new_payload AS `Giá trị mới`
FROM ADMIN_AUDIT_LOGS al
JOIN USERS u ON al.admin_id = u.id
LEFT JOIN PARKING_SPOTS ps 
    ON al.target_table = 'PARKING_SPOTS' AND al.target_record_id = ps.id
LEFT JOIN EV_CHARGERS ec   
    ON al.target_table = 'EV_CHARGERS'   AND al.target_record_id = ec.id
LEFT JOIN LOCATIONS l      
    ON l.id = COALESCE(ps.location_id, ec.location_id)
WHERE al.target_table IN ('PARKING_SPOTS', 'EV_CHARGERS')
ORDER BY al.created_at DESC, al.id DESC;