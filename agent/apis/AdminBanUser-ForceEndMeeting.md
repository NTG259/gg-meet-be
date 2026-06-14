# API Docs: Ban/Unban User & Force Tắt Phòng Họp (Admin)

## Phân quyền

- Tất cả endpoint yêu cầu JWT token có role `ADMIN`.
- Trả về `401 Unauthorized` nếu chưa đăng nhập.
- Trả về `403 Forbidden` nếu role không phải `ADMIN`.

---

## 1. Ban tài khoản user

```
POST /api/admin/users/{userId}/ban
```

Admin khóa tài khoản user. Các token hiện tại bị vô hiệu hóa ngay lập tức, user không thể đăng nhập cho đến khi được unban.

**Cơ chế:**
- Set `isBanned = true`
- Tăng `accessTokenVersion` → tất cả access token hiện tại bị từ chối
- Xóa `refreshToken` trong DB → không thể refresh lấy token mới

### Headers

```
Authorization: Bearer <admin_token>
```

### Response

**200 OK**
```json
{
    "data": {
        "id": 5,
        "email": "user@example.com",
        "fullName": "Nguyen Van A",
        "role": "USER",
        "status": "ACTIVE",
        "isBanned": true,
        "avatar": null,
        "meetingCount": 3,
        "createdAt": "2024-01-01T10:00:00"
    },
    "message": "User banned successfully",
    "errors": null,
    "status": 200
}
```

### Error Codes

| HTTP | Message |
|------|---------|
| 404 | User not found |
| 403 | Account has been banned (user đã bị ban từ trước) |

---

## 2. Unban tài khoản user

```
POST /api/admin/users/{userId}/unban
```

Gỡ ban tài khoản user. User có thể đăng nhập lại bình thường.

### Headers

```
Authorization: Bearer <admin_token>
```

### Response

**200 OK**
```json
{
    "data": {
        "id": 5,
        "email": "user@example.com",
        "fullName": "Nguyen Van A",
        "role": "USER",
        "status": "ACTIVE",
        "isBanned": false,
        "avatar": null,
        "meetingCount": 3,
        "createdAt": "2024-01-01T10:00:00"
    },
    "message": "User unbanned successfully",
    "errors": null,
    "status": 200
}
```

### Error Codes

| HTTP | Message |
|------|---------|
| 404 | User not found |
| 400 | Invalid input data (user chưa bị ban) |

---

## 3. Cập nhật role người dùng

```
PUT /api/admin/users/{userId}
```

Chỉ cho phép admin thay đổi `role` của user.

### Headers

```
Authorization: Bearer <admin_token>
Content-Type: application/json
```

### Request Body

```json
{
    "role": "ADMIN"
}
```

| Field | Bắt buộc | Mô tả |
|-------|----------|-------|
| `role` | Có | `"USER"` hoặc `"ADMIN"` |

### Response

**200 OK**
```json
{
    "data": {
        "id": 2,
        "email": "user@example.com",
        "fullName": "Tran Thi B",
        "role": "ADMIN",
        "status": "ACTIVE",
        "isBanned": false,
        "avatar": null,
        "meetingCount": 5,
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
| 400 | Invalid input data (`role` bỏ trống hoặc không hợp lệ) |

---

## 4. Force tắt phòng họp

```
DELETE /api/admin/meetings/{meetingId}
```

Admin buộc kết thúc một phòng họp đang ACTIVE. Tất cả participant nhận thông báo `MEETING_ENDED` qua WebSocket và bị đẩy ra khỏi phòng.

**Cơ chế:**
1. Xóa phòng trên LiveKit (ngắt kết nối tất cả participant)
2. Cập nhật `status = ENDED`, ghi nhận `endTime`
3. Gửi WebSocket event `MEETING_ENDED` tới topic `/topic/meeting/{meetingCode}`

### Headers

```
Authorization: Bearer <admin_token>
```

### Response

**200 OK**
```json
{
    "data": null,
    "message": "Meeting force-ended successfully",
    "errors": null,
    "status": 200
}
```

### Error Codes

| HTTP | Message |
|------|---------|
| 404 | Meeting room not found |
| 400 | Meeting has already ended (phòng không ở trạng thái ACTIVE) |

---

## WebSocket Event khi force-end meeting

Phía client lắng nghe topic `/topic/meeting/{meetingCode}` sẽ nhận:

```json
{
    "action": "MEETING_ENDED",
    "meetingCode": "abc12"
}
```

> Xử lý giống hệt khi host tự kết thúc phòng — redirect về màn hình chính.

---

## Tóm tắt endpoint mới

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/api/admin/users/{userId}/ban` | Ban tài khoản user |
| `POST` | `/api/admin/users/{userId}/unban` | Gỡ ban tài khoản user |
| `PUT` | `/api/admin/users/{userId}` | Cập nhật role user |
| `DELETE` | `/api/admin/meetings/{meetingId}` | Force tắt phòng họp |

---

## Các file đã thêm / sửa

| File | Thay đổi |
|------|----------|
| `domain/entity/User.java` | Thêm field `isBanned` (Boolean, default `false`) |
| `domain/response/AdminUserResponseDTO.java` | Thêm field `isBanned` |
| `domain/request/AdminUpdateUserRequest.java` | Chỉ giữ field `role` |
| `utils/security/CustomUsersDetail.java` | Thêm field `banned`, override `isAccountNonLocked()` |
| `utils/security/CustomUserDetailsService.java` | Truyền `isBanned` vào `CustomUsersDetail` |
| `config/SecurityConfig.java` | Kiểm tra `isBanned` trong `validateAccessToken()` |
| `utils/errors/ErrorCode.java` | Thêm `USER_BANNED` (403) |
| `service/admin/AdminUserService.java` | Thêm `banUser()`, `unbanUser()`; mở rộng `updateUser()` |
| `controller/AdminUserController.java` | Thêm endpoint `/ban`, `/unban` |
| `service/admin/AdminMeetingService.java` | Thêm `forceEndMeeting()`, inject `LiveKitService`, `NotificationService` |
| `controller/AdminMeetingController.java` | Thêm endpoint `DELETE /{meetingId}` |
