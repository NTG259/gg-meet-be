# Tài Liệu Chức Năng: Cài Đặt Chia Sẻ Màn Hình (Backend)

## 1. Tổng Quan

Host có thể bật/tắt chế độ **"Cho phép participant tự do chia sẻ màn hình"**:

| `allowParticipantShareScreen` | Ý nghĩa |
|-------------------------------|---------|
| `true` (mặc định) | Người tham gia tự do chia sẻ màn hình mà không bị chặn. |
| `false` | Quyền chia sẻ bị khóa. Người tham gia phải gửi yêu cầu lên Backend và chờ Host duyệt. |

Khi Host đổi cấu hình sang `false`, Backend sẽ tự động **force-stop** toàn bộ luồng chia sẻ màn hình đang chạy của participant thông qua LiveKit.

---

## 2. API

### 2a. Host thay đổi cài đặt chia sẻ màn hình

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
{ "allowParticipantShareScreen": false }
```

**Response (200 OK):**
```json
{
    "data": null,
    "message": "Room settings updated",
    "error": null,
    "statusCode": 200
}
```

---

### 2b. Participant gửi yêu cầu xin share màn hình

> Chỉ gọi khi `allowParticipantShareScreen = false`

```
POST /api/meetings/{meetingCode}/screen-share-requests
```

**Headers:**
```
Meeting-Token: <meeting_token>
```

**Response (200 OK):**
```json
{
    "data": null,
    "message": "Screen share request sent",
    "error": null,
    "statusCode": 200
}
```

Backend sẽ xử lý và broadcast event `SCREEN_SHARE_REQUESTED` tới **toàn bộ phòng** (để Host nhận được).

---

### 2c. Host duyệt yêu cầu (Approve)

```
POST /api/meetings/{meetingCode}/screen-share-requests/{requesterId}/approve
```

**Headers:**
```
Meeting-Token: <meeting_token>
```

**Response (200 OK):**
```json
{
    "data": null,
    "message": "Screen share approved",
    "error": null,
    "statusCode": 200
}
```

Backend thực hiện:
1. Cập nhật Participant Metadata trên LiveKit của `requesterId` với `canShareScreenOverride: true`.
2. Gửi event `SCREEN_SHARE_APPROVED` riêng tới participant được duyệt thông qua WebSocket.

---

### 2d. Host từ chối yêu cầu (Reject)

```
POST /api/meetings/{meetingCode}/screen-share-requests/{requesterId}/reject
```

**Headers:**
```
Meeting-Token: <meeting_token>
```

**Response (200 OK):**
```json
{
    "data": null,
    "message": "Screen share rejected",
    "error": null,
    "statusCode": 200
}
```

Backend sẽ gửi event `SCREEN_SHARE_REJECTED` riêng tới participant bị từ chối qua WebSocket.

---

### 2e. Host ép dừng màn hình đang share (Force Stop)

```
POST /api/meetings/{meetingCode}/screen-share/{targetId}/stop
```

**Headers:**
```
Meeting-Token: <meeting_token>
```

**Response (200 OK):**
```json
{
    "data": null,
    "message": "Screen share stopped",
    "error": null,
    "statusCode": 200
}
```

Backend thực hiện:
1. Yêu cầu LiveKit server-side mute track `SCREEN_SHARE` của `targetId`.
2. Thu hồi cờ `canShareScreenOverride` trong Participant Metadata.
3. Broadcast event `SCREEN_SHARE_STOPPED` tới toàn bộ phòng qua WebSocket.

---

## 3. Các sự kiện Realtime (WebSocket + LiveKit) Backend phát ra

### 3a. STOMP Events (WebSocket)

| Tên Event | Nơi nhận (Topic) | Nội dung JSON |
|-----------|-----------------|---------------|
| `ROOM_SETTINGS_CHANGED` | `/topic/meeting/{meetingCode}` | Gửi cấu hình phòng mới (VD: `allowParticipantShareScreen`). |
| `SCREEN_SHARE_REQUESTED` | `/topic/meeting/{meetingCode}` | Thông báo có user muốn share (`targetParticipantId`, `targetName`). |
| `SCREEN_SHARE_APPROVED` | `/topic/meeting/{meetingCode}/participant/{requesterId}` | Thông báo duyệt thành công. |
| `SCREEN_SHARE_REJECTED` | `/topic/meeting/{meetingCode}/participant/{requesterId}` | Thông báo từ chối. |
| `SCREEN_SHARE_STOPPED` | `/topic/meeting/{meetingCode}` | Thông báo luồng màn hình của `targetParticipantId` đã bị ép dừng. |

### 3b. LiveKit Metadata Updates

- **Room Metadata (`RoomMetadataChanged`)**: Khi Host gọi API settings, Backend cập nhật Room Metadata để LiveKit tự động broadcast trạng thái mới.
- **Participant Metadata (`ParticipantMetadataChanged`)**: Khi Host approve, Backend gắn thêm quyền tạm thời `canShareScreenOverride: true` cho participant đó thông qua LiveKit.
