# API Docs: Tắt Mic / Camera Từng Người Dùng

## Endpoint

```
POST /api/meetings/{meetingCode}/participants/{targetId}/mute
```

## Headers

```
Meeting-Token: <meeting_token>
Content-Type: application/json
```

## Request Body

```json
{
    "trackType": "AUDIO"
}
```

> `trackType`: `"AUDIO"` hoặc `"VIDEO"`

## Response

**200 OK**
```json
{
    "data": null,
    "message": "Participant track muted successfully",
    "error": null,
    "statusCode": 200
}
```

## Error Codes

| HTTP | Message |
|------|---------|
| 401 | User is not authenticated |
| 400 | Only the room owner has permission |
| 404 | User is not in the current meeting room |
| 400 | Participant does not have an active track of the requested type |
| 500 | Mute operation failed |

## Realtime Broadcast

Không cần lắng nghe thêm event từ STOMP. LiveKit tự động broadcast sự kiện **`TrackMuted`** đến toàn bộ client trong phòng sau khi mute thành công.
