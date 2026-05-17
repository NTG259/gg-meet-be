# Tài Liệu Tích Hợp Frontend: Tắt Mic Toàn Bộ & Chặn Tự Mở Mic

## 1. Tổng Quan

Host có thể bật/tắt chế độ **"Chỉ Host được mở mic"**:

- Khi **bật** (`allowParticipantUnmute: false`):
  - Toàn bộ participant (trừ Host) bị tắt mic ngay lập tức (server-side mute).
  - Nút "Bật Mic" trên giao diện của participant bị **disabled**.
  - Participant **không thể** tự mở mic.

- Khi **tắt** (`allowParticipantUnmute: true`):
  - Participant vẫn đang bị tắt mic, nhưng nút "Bật Mic" được **enabled** trở lại.
  - Participant có thể tự bấm mở mic nếu muốn.

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

## 3. Nhận Trạng Thái Realtime (WebSocket)

Backend gửi 2 tín hiệu đồng thời khi Host thay đổi cài đặt:

### 3a. LiveKit Room Metadata (ưu tiên dùng cái này)

Khi `updateRoomMetadata` được gọi, LiveKit Server **tự động broadcast** event xuống tất cả client đang kết nối trong phòng.

**Lắng nghe trong LiveKit Room:**
```typescript
room.on(RoomEvent.RoomMetadataChanged, (metadata: string) => {
    const settings = JSON.parse(metadata);
    const allowUnmute = settings.allowParticipantUnmute; // true | false | undefined
    if (allowUnmute !== undefined) {
        updateMuteButtonState(allowUnmute);
    }
});
```

> **Lưu ý:** Khi người dùng mới join phòng, `room.metadata` đã chứa sẵn giá trị hiện tại — client tự đọc ngay khi connect mà không cần gọi thêm API.

### 3b. WebSocket nội bộ (STOMP)

Topic: `/topic/meeting/{meetingCode}`

**Message nhận được:**
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

## 4. Logic UI Cần Xử Lý

### 4a. Khi nhận event thay đổi cài đặt

```typescript
function handleRoomSettingsChanged(allowParticipantUnmute: boolean, isLocalParticipantHost: boolean) {
    if (isLocalParticipantHost) return; // Host không bị ảnh hưởng

    const micButton = document.getElementById('mic-toggle-btn');
    if (!allowParticipantUnmute) {
        micButton.disabled = true;
        micButton.title = 'Host đã tắt quyền tự mở mic';
        // Ẩn hoặc disable nút "Xin phép mở mic" nếu có
    } else {
        micButton.disabled = false;
        micButton.title = '';
    }
}
```

### 4b. Khi người dùng mới join (Late Joiner)

Đọc `room.metadata` ngay sau khi `room.connect()` thành công:

```typescript
room.on(RoomEvent.Connected, () => {
    if (room.metadata) {
        const settings = JSON.parse(room.metadata);
        if (settings.allowParticipantUnmute === false) {
            disableMicButton();
        }
    }
});
```

### 4c. Giao diện phía Host

Host cần có một toggle/switch để điều khiển chế độ này:

```typescript
async function toggleMuteAll(checked: boolean) {
    // checked = true  → allowParticipantUnmute: false (bật chế độ khóa)
    // checked = false → allowParticipantUnmute: true  (tắt chế độ khóa)
    await fetch(`/api/meetings/${meetingCode}/settings`, {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
            'Meeting-Token': meetingToken,
        },
        body: JSON.stringify({ allowParticipantUnmute: !checked }),
    });
}
```

---

## 5. Lưu Ý Quan Trọng

1. **Phân biệt bị mute bởi server vs. tự mute**: Khi backend gọi `forceMuteAllNonHosts`, LiveKit sẽ server-side mute track. Client nhận event `TrackMuted` từ LiveKit — đây là hành vi bình thường, client không cần xử lý thêm ngoài việc cập nhật UI.

2. **Khi `allowParticipantUnmute` trở lại `true`**: Participant vẫn đang bị mute (server-side mute không tự gỡ). Client cần cho phép participant tự bấm unmute — LiveKit sẽ xử lý việc publish lại track khi participant bấm mở mic.

3. **Metadata Room có thể chứa nhiều key**: Một lần update chỉ ghi đè toàn bộ metadata. Hiện tại backend ghi riêng từng setting (`allowParticipantShareScreen` hoặc `allowParticipantUnmute`) — client nên **merge** khi parse để không mất key cũ:
   ```typescript
   room.on(RoomEvent.RoomMetadataChanged, (metadata: string) => {
       const incoming = JSON.parse(metadata);
       roomSettings = { ...roomSettings, ...incoming }; // merge, không replace
   });
   ```

4. **Chỉ HOST mới thấy và dùng được toggle**: Kiểm tra `localParticipant.metadata.role === 'HOST'` trước khi render control.
