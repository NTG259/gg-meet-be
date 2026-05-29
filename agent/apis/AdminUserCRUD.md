# API Docs: Dashboard Quản Lý Người Dùng (Admin User CRUD)

## Phân quyền

- Tất cả endpoint yêu cầu JWT token có role `ADMIN`.
- Trả về `401 Unauthorized` nếu chưa đăng nhập.
- Trả về `403 Forbidden` nếu role không phải `ADMIN`.

---

## 1. Lấy danh sách người dùng

```
GET /api/admin/users
```

### Headers

```
Authorization: Bearer <admin_token>
```

### Query Parameters

| Param | Bắt buộc | Mặc định | Mô tả |
|-------|----------|----------|-------|
| `page` | Không | `0` | Số trang |
| `size` | Không | `10` | Số record mỗi trang |
| `search` | Không | — | Tìm kiếm theo tên hoặc email |
| `role` | Không | — | Lọc role: `USER` hoặc `ADMIN` |
| `status` | Không | — | Lọc trạng thái: `ACTIVE` hoặc `INACTIVE` |

### Response

**200 OK**
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
                "avatar": "https://...",
                "meetingCount": 12,
                "createdAt": "2024-01-01T10:00:00"
            }
        ],
        "page": 0,
        "size": 10,
        "totalElements": 50,
        "totalPages": 5,
        "last": false
    },
    "message": "Fetched users successfully",
    "errors": null,
    "status": 200
}
```

> `meetingCount`: số cuộc họp user là host.  
> `role`/`status` sai giá trị trả `400 Bad Request`.

---

## 2. Xem chi tiết một người dùng

```
GET /api/admin/users/{userId}
```

### Headers

```
Authorization: Bearer <admin_token>
```

### Response

**200 OK**
```json
{
    "data": {
        "id": 1,
        "email": "user@example.com",
        "fullName": "Nguyen Van A",
        "role": "USER",
        "status": "ACTIVE",
        "avatar": "https://...",
        "meetingCount": 12,
        "createdAt": "2024-01-01T10:00:00"
    },
    "message": "User details fetched",
    "errors": null,
    "status": 200
}
```

### Error Codes

| HTTP | Message |
|------|---------|
| 404 | User not found |

---

## 3. Tạo mới người dùng

```
POST /api/admin/users
```

### Headers

```
Authorization: Bearer <admin_token>
Content-Type: application/json
```

### Request Body

```json
{
    "email": "newuser@example.com",
    "fullName": "Tran Thi B",
    "password": "Password123!",
    "role": "USER"
}
```

> `role`: `"USER"` hoặc `"ADMIN"` (mặc định `"USER"` nếu bỏ trống)

### Response

**201 Created**
```json
{
    "data": {
        "id": 2,
        "email": "newuser@example.com",
        "fullName": "Tran Thi B",
        "role": "USER",
        "status": "ACTIVE",
        "avatar": null,
        "meetingCount": 0,
        "createdAt": "2024-06-01T09:00:00"
    },
    "message": "User created successfully",
    "errors": null,
    "status": 201
}
```

### Error Codes

| HTTP | Message |
|------|---------|
| 409 | Email is already in use |
| 400 | Invalid input data (email sai format, password < 8 ký tự, ...) |

---

## 4. Cập nhật thông tin người dùng

```
PUT /api/admin/users/{userId}
```

### Headers

```
Authorization: Bearer <admin_token>
Content-Type: application/json
```

### Request Body

```json
{
    "fullName": "Tran Thi B Updated",
    "role": "ADMIN",
    "status": "INACTIVE"
}
```

> Tất cả field đều là optional. Chỉ gửi field muốn cập nhật.  
> `status`: `"ACTIVE"` hoặc `"INACTIVE"`  
> `role`: `"USER"` hoặc `"ADMIN"`

### Response

**200 OK**
```json
{
    "data": {
        "id": 2,
        "email": "newuser@example.com",
        "fullName": "Tran Thi B Updated",
        "role": "ADMIN",
        "status": "INACTIVE",
        "avatar": null,
        "meetingCount": 12,
        "createdAt": "2024-06-01T09:00:00"
    },
    "message": "User updated successfully",
    "errors": null,
    "status": 200
}
```

### Error Codes

| HTTP | Message |
|------|---------|
| 404 | User not found |
| 400 | Invalid status |

---

## 5. Xóa người dùng (Soft Delete)

```
DELETE /api/admin/users/{userId}
```

Không xóa cứng khỏi database. Hệ thống set `isDeleted = true` và `enabled = false` trên bản ghi user.

### Headers

```
Authorization: Bearer <admin_token>
```

### Response

**200 OK**
```json
{
    "data": null,
    "message": "User deleted (deactivated) successfully",
    "errors": null,
    "status": 200
}
```

### Error Codes

| HTTP | Message |
|------|---------|
| 404 | User not found |

---

## Các file đã thêm / sửa

| File | Thay đổi |
|------|----------|
| `domain/entity/User.java` | Thêm field `isDeleted` (Boolean, default `false`) |
| `repository/UserRepository.java` | Thêm query list admin hỗ trợ `search`, `role`, `status` |
| `repository/MeetingRepository.java` | Thêm query count meeting theo host để trả `meetingCount` |
| `domain/response/AdminUserResponseDTO.java` | DTO response, có `meetingCount` |
| `domain/request/AdminCreateUserRequest.java` | Request tạo user |
| `domain/request/AdminUpdateUserRequest.java` | Request cập nhật user |
| `service/admin/AdminUserService.java` | Service xử lý CRUD |
| `controller/AdminUserController.java` | Controller tại `/api/admin/users` |
| `config/SecurityConfig.java` | Thêm rule `hasRole("ADMIN")` cho `/api/admin/**` |
