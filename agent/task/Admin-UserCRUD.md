# Tài Liệu API: Dashboard Quản Lý Người Dùng (CRUD User)

## 1. Tổng Quan

Đây là tài liệu mô tả các API phục vụ cho màn hình Dashboard quản trị hệ thống, cụ thể là quản lý thông tin người dùng (User CRUD). 

**Yêu cầu phân quyền nghiêm ngặt:**
- Chỉ những tài khoản có vai trò (role) là `ADMIN` mới có quyền gọi các API này.
- Tất cả các endpoint quản trị sẽ nằm dưới tiền tố `/api/admin/...`.
- Bất kỳ tài khoản nào có role khác (ví dụ: `USER`) hoặc không đăng nhập khi gọi API này sẽ nhận được mã lỗi `403 Forbidden` hoặc `401 Unauthorized`.

---

## 2. Danh Sách API (User CRUD)

### 2a. Lấy danh sách người dùng (Read - Pagination)

Lấy danh sách toàn bộ người dùng trong hệ thống có phân trang và hỗ trợ tìm kiếm cơ bản.

- **Endpoint:** `GET /api/admin/users`
- **Query Parameters:**
  - `page` (optional): Số trang (mặc định: 0)
  - `size` (optional): Số lượng record trên một trang (mặc định: 10)
  - `search` (optional): Từ khóa tìm kiếm theo tên hoặc email.
- **Headers:** `Authorization: Bearer <admin_token>`
- **Response (200 OK):**
```json
{
    "data": {
        "content": [
            {
                "id": 1,
                "email": "user@example.com",
                "fullName": "Nguyen Van A",
                "role": "USER",
                "status": "ACTIVE",
                "createdAt": "2024-01-01T10:00:00"
            }
        ],
        "pageNo": 0,
        "pageSize": 10,
        "totalElements": 50,
        "totalPages": 5,
        "last": false
    },
    "message": "Fetched users successfully",
    "error": null,
    "statusCode": 200
}
```

---

### 2b. Xem chi tiết một người dùng (Read - Detail)

Lấy toàn bộ thông tin chi tiết của một user cụ thể.

- **Endpoint:** `GET /api/admin/users/{userId}`
- **Headers:** `Authorization: Bearer <admin_token>`
- **Response (200 OK):**
```json
{
    "data": {
        "id": 1,
        "email": "user@example.com",
        "fullName": "Nguyen Van A",
        "role": "USER",
        "status": "ACTIVE",
        "avatar": "https://...",
        "createdAt": "2024-01-01T10:00:00"
    },
    "message": "User details fetched",
    "error": null,
    "statusCode": 200
}
```
- **Lỗi (404 Not Found):** Nếu không tìm thấy ID.

---

### 2c. Tạo mới người dùng (Create)

Admin tạo mới một tài khoản người dùng trực tiếp từ Dashboard.

- **Endpoint:** `POST /api/admin/users`
- **Headers:** 
  - `Authorization: Bearer <admin_token>`
  - `Content-Type: application/json`
- **Request Body:**
```json
{
    "email": "newuser@example.com",
    "fullName": "Tran Thị B",
    "password": "Password123!",
    "role": "USER" // hoặc "ADMIN"
}
```
- **Response (201 Created):**
```json
{
    "data": {
        "id": 2,
        "email": "newuser@example.com",
        "fullName": "Tran Thị B",
        "role": "USER",
        "status": "ACTIVE"
    },
    "message": "User created successfully",
    "error": null,
    "statusCode": 201
}
```
- **Lỗi (400 Bad Request):** Nếu Email đã tồn tại hoặc format mật khẩu không hợp lệ.

---

### 2d. Cập nhật thông tin người dùng (Update)

Admin có thể sửa thông tin người dùng (Đổi tên, đổi Role, đổi trạng thái).

- **Endpoint:** `PUT /api/admin/users/{userId}`
- **Headers:** 
  - `Authorization: Bearer <admin_token>`
  - `Content-Type: application/json`
- **Request Body:**
```json
{
    "fullName": "Tran Thị B Updated",
    "role": "ADMIN",
    "status": "INACTIVE"
}
```
- **Response (200 OK):**
```json
{
    "data": {
        "id": 2,
        "email": "newuser@example.com",
        "fullName": "Tran Thị B Updated",
        "role": "ADMIN",
        "status": "INACTIVE"
    },
    "message": "User updated successfully",
    "error": null,
    "statusCode": 200
}
```

---

### 2e. Xóa / Khóa người dùng (Delete)

Để đảm bảo an toàn dữ liệu, hệ thống thường dùng **Soft Delete** (vô hiệu hóa/đổi trạng thái) thay vì xóa cứng (Hard Delete) khỏi database. Bổ sung field isDeleted để lưu trữ trạng thái xóa.

- **Endpoint:** `DELETE /api/admin/users/{userId}`
- **Headers:** `Authorization: Bearer <admin_token>`
- **Response (200 OK):**
```json
{
    "data": null,
    "message": "User deleted (deactivated) successfully",
    "error": null,
    "statusCode": 200
}
```

---

## 3. Kiến Trúc Phân Quyền (Backend Security)

Trong Spring Boot Security, để chặn chặt chẽ các API này, Backend sẽ thực hiện:

1. Phân quyền trên Security Filter Chain:
   ```java
   http.authorizeHttpRequests(auth -> auth
       .requestMatchers("/api/admin/**").hasRole("ADMIN")
       .anyRequest().authenticated()
   );
   ```
