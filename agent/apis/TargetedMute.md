# API Docs: Tắt Mic / Camera Từng Người Dùng

## WebSocket

**Destination:** `/app/meeting/mute`

**Yêu cầu xác thực:** Meeting-Token phải được gửi trong header `Meeting-Token` lúc kết nối WebSocket (STOMP CONNECT).

## Request Payload

```json
{
    "meetingCode": "ABC123",
    "targetParticipantId": 42,
    "trackType": "AUDIO"
}
```

| Field | Type | Bắt buộc | Mô tả |
|-------|------|----------|-------|
| `meetingCode` | `string` | ✅ | Mã phòng họp |
| `targetParticipantId` | `number` | ✅ | `participantId` (PK bảng `meeting_participants`) của người bị tắt |
| `trackType` | `"AUDIO"` \| `"VIDEO"` | ✅ | Loại track cần tắt |

> **Lưu ý:** `targetParticipantId` là `participantId` của bảng `meeting_participants`, **không phải** `userId` của bảng `users`.

## Phân quyền

Chỉ HOST mới được phép gửi lệnh này. Server tự động xác định host từ WebSocket session.

## Error

Nếu xảy ra lỗi, server đóng session hoặc không phản hồi. Các trường hợp lỗi:

| Nguyên nhân | Mô tả |
|-------------|-------|
| Không phải HOST | Không có quyền thực hiện |
| `targetParticipantId` không tồn tại | Người dùng không có trong phòng |
| Track không active | Participant không có track loại đó đang bật |

## Realtime Broadcast

Không cần lắng nghe thêm event từ STOMP. LiveKit tự động broadcast sự kiện **`TrackMuted`** đến toàn bộ client trong phòng sau khi mute thành công.
