🚗⚡ EV Charging & Parking Management System

Đồ án môn Cơ sở dữ liệu — Hệ thống quản lý đặt chỗ tập trung cho trạm sạc xe điện và bãi đỗ xe đô thị, tích hợp định giá linh hoạt theo khung giờ cao điểm.

📋 Tổng quan
Hệ thống backend quản lý vòng đời đặt chỗ (booking) cho hai loại tài nguyên đô thị:
- Trạm sạc xe điện — vận hành tự phục vụ 100%, không có nhân viên giám sát.
- Bãi đỗ xe — vận hành tự động qua hệ thống quét mã QR, không có bảo vệ gác cổng.

Trọng tâm đồ án là thiết kế Cơ sở dữ liệu quan hệ, xử lý dữ liệu thời gian (temporal data), tính phí theo thời lượng sử dụng thực tế, và biểu giá linh động theo khung giờ.

🎯 Mục tiêu kỹ thuật
Quản lý vòng đời đặt chỗ thống nhất cho cả 2 loại dịch vụ (Booking → Check-in → Check-out → Hóa đơn).
Tính phí tự động theo thuật toán cắt lớp thời gian (Time-Slicing) và phân bổ điện năng theo tỷ lệ (Proportional kWh Allocation).
Đảm bảo toàn vẹn dữ liệu trong môi trường nhiều người dùng truy cập đồng thời (Concurrency Control).
Tìm kiếm trạm sạc/bãi đỗ gần nhất theo tọa độ (Haversine + Bounding Box pre-filter).

🏗️ Kiến trúc & Công nghệ
Thành phần	Công nghệ
Backend	Java Spring Boot
Cơ sở dữ liệu	MySQL 8.0+
ORM	Spring Data JPA
API Testing	Postman (giả lập phần cứng/IoT)
