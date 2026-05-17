# Tài Liệu Chức Năng: Tắt Mic Toàn Bộ & Chặn Tự Mở Mic (Backend)

## 1. Tổng Quan

Host có thể bật/tắt chế độ **"Chỉ Host được mở mic"**:

- Khi **bật** (`allowParticipantUnmute: false`):
  - Backend xử lý tắt mic (server-side mute) toàn bộ participant (trừ Host) ngay lập tức.
  - Cập nhật trạng thái `allowParticipantUnmute` thành `false` trong hệ thống để chặn người dùng tự mở mic.

- Khi **tắt** (`allowParticipantUnmute: true`):
  - Cập nhật trạng thái `allowParticipantUnmute` thành `true`.
  - Backend không tự động gỡ mute (participant vẫn bị mute), nhưng hệ thống sẽ cho phép participant tự mở mic nếu có yêu cầu.

---

## 2. API

### Cập nhật cài đặt phòng

```
PATCH /api/meetings/{meetingCode}/settings
```

**Headers:**
```
Meeting-Token: <meeting_token>
Content-Type: application/json
```

**Request Body:**
```json
{
    "allowParticipantUnmute": false
}
```

> Có thể gửi kèm `allowParticipantShareScreen` trong cùng một request nếu cần.

**Response (200 OK):**
```json
{
    "data": null,
    "message": "Room settings updated",
    "error": null,
    "statusCode": 200
}
```

**Lỗi có thể gặp:**
| HTTP Status | Ý nghĩa |
|-------------|---------|
| 401 | Meeting-Token không hợp lệ hoặc thiếu |
| 403 | Người gọi không phải HOST |
| 500 | Lỗi thao tác với LiveKit |

---

## 3. Realtime Events do Backend gửi

Backend phát sinh các luồng dữ liệu (events) đồng thời khi Host thay đổi cài đặt:

### 3a. LiveKit Room Metadata 

Khi gọi API thành công, Backend gọi `updateRoomMetadata` để LiveKit Server **tự động broadcast** event xuống tất cả client trong phòng.
Dữ liệu Metadata có chứa:
```json
{
    "allowParticipantUnmute": false
}
```

### 3b. WebSocket nội bộ (STOMP)

Topic: `/topic/meeting/{meetingCode}`

**Message Backend gửi ra:**
```json
{
    "meetingCode": "abc12",
    "action": "ROOM_SETTINGS_CHANGED",
    "extra": {
        "allowParticipantUnmute": false
    }
}
```

---

## 4. Hành vi hệ thống phía Server

1. **Phân biệt bị mute bởi server vs. tự mute**: Khi backend gọi `forceMuteAllNonHosts`, hệ thống LiveKit sẽ server-side mute track của người dùng. 
2. **Quản lý Metadata Room**: Khi Backend thực hiện cập nhật cài đặt, Backend sẽ tự động merge cấu hình `allowParticipantUnmute` vào metadata hiện tại của phòng (bảo tồn các setting khác như screen share).
