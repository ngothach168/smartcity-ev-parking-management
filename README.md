# Hệ Thống Quản Lý Bãi Đỗ Xe & Trạm Sạc Điện Thông Minh

Ứng dụng Backend sử dụng Java Spring Boot (JDK 21) và cơ sở dữ liệu MySQL 8.0.

## 🛠 Yêu cầu cài đặt

- **JDK 21**
- **MySQL Server 8.0** (kèm MySQL Workbench)
- **IntelliJ IDEA**
- **Git**

## 🚀 Hướng dẫn cài đặt & Chạy

### Bước 1: Tải code về máy

Mở Terminal/CMD và chạy:

```bash
git clone https://github.com/ngothach168/smartcity-ev-parking-management.git
```

### Bước 2: Khởi tạo Database

Mở MySQL Workbench và chạy lần lượt 3 file SQL trong thư mục `database/` theo đúng thứ tự:

1. `01_schema_ddl.sql`
2. `02_mock_data.sql`
3. `03_triggers_views.sql`

### Bước 3: Cấu hình Database

1. Truy cập thư mục `backend/src/main/resources/`.
2. Tìm file `application.properties.example`.
3. Sao chép file và đổi tên bản sao thành `application.properties`.
4. Mở file `application.properties` và cập nhật tài khoản, mật khẩu MySQL (`username`, `password`) theo cấu hình trên máy của bạn.

### Bước 4: Chạy ứng dụng

1. Mở thư mục `backend` bằng IntelliJ IDEA.
2. Tìm file `DemoApplication.java` tại `src/main/java/com/example/demo/`.
3. Bấm **Run** để khởi chạy ứng dụng.

## 🌐 Truy cập

Sau khi ứng dụng khởi chạy thành công, mở trình duyệt tại:

[http://localhost:8080](http://localhost:8080)

### Tài khoản Admin mặc định

- **Số điện thoại:** `0900000001`
- **Mật khẩu:** `123456`
