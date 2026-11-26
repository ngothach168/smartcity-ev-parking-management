# 🚗 Smart City EV Charging & Parking Management

Hệ thống quản lý bãi đỗ xe và trạm sạc xe điện thông minh.

## 📌 Giới thiệu công nghệ

- **Backend:** Java Spring Boot (JDK 21)
- **Database:** MySQL Server 8.0
- **IDE khuyên dùng:** IntelliJ IDEA
- **Quản lý mã nguồn:** Git

---

## 🛠 Yêu cầu cài đặt trước

Hãy đảm bảo máy tính của bạn đã cài đặt các phần mềm sau:

- [JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)
- [MySQL Server 8.0 & MySQL Workbench](https://dev.mysql.com/downloads/installer/)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
- [Git](https://git-scm.com/downloads)

---

## 🚀 Hướng dẫn cài đặt & Chạy dự án

### Bước 1: Tải mã nguồn về máy

Mở Terminal/CMD và chạy lệnh sau:

```bash
git clone https://github.com/ngothach168/smartcity-ev-parking-management.git
```

### Bước 2: Khởi tạo Cơ sở dữ liệu

Mở **MySQL Workbench**, tạo một vùng làm việc mới và chạy lần lượt **5 file SQL** trong thư mục `database/` theo đúng thứ tự sau:

1. `01_schema_ddl.sql` (Khởi tạo cấu trúc bảng)
2. `02_mock_data_dml.sql` (Chèn dữ liệu mẫu)
3. `03_triggers_views.sql` (Tạo Triggers, View và Event)
4. `04_complex_queries.sql` (Các truy vấn phức tạp - Tùy chọn)
5. `05_rbac.sql` (Phân quyền tài khoản Database `ev_app` cho ứng dụng )

### Bước 3: Cấu hình kết nối Database

1. Truy cập vào thư mục: `backend/src/main/resources/`
2. Tìm file `application.properties.example`.
3. Sao chép file đó và đổi tên bản sao thành `application.properties`.
4. Mở file `application.properties` vừa tạo.

   *(Nếu bạn đã chạy file `05_rbac.sql` ở Bước 2, tài khoản và mật khẩu đã được điền sẵn là `ev_app` / `AppPass123$`. Nếu dùng tài khoản khác, hãy sửa lại `username` và `password` cho đúng với máy của bạn).*

### Bước 4: Khởi chạy ứng dụng

1. Mở thư mục `backend` bằng **IntelliJ IDEA**.
2. Tìm file `DemoApplication.java` tại đường dẫn `src/main/java/com/example/demo/`.
3. Bấm nút **Run** (biểu tượng tam giác màu xanh) để khởi chạy.

---

## 🌐 Truy cập ứng dụng

Sau khi ứng dụng khởi chạy thành công, mở trình duyệt và truy cập:

👉 [**http://localhost:8080**](http://localhost:8080)

### Tài khoản Admin mặc định (dùng để test)

- **Số điện thoại:** `0900000001`
- **Mật khẩu:** `123456`
