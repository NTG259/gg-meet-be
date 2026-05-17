# Tài Liệu Tích Hợp Frontend: Cài Đặt Chia Sẻ Màn Hình (Screen Share Setting)

## 1. Tổng Quan

Host có thể bật/tắt chế độ **"Cho phép participant tự do chia sẻ màn hình"**:

| `allowParticipantShareScreen` | Ý nghĩa |
|-------------------------------|---------|
| `true` (mặc định) | Tất cả mọi người đều tự share màn hình tự do — **không cần xin phép** |
| `false` | Participant **phải gửi yêu cầu**, Host duyệt mới được share |

Khi Host đổi sang `false`, backend tự động **force-stop** toàn bộ luồng chia sẻ màn hình đang chạy của participant ngay lập tức.

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

**Lỗi:**
| HTTP | Ý nghĩa |
|------|---------|
| 401 | Meeting-Token không hợp lệ |
| 403 | Người gọi không phải HOST |
| 500 | Lỗi thao tác với LiveKit |

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

Backend sẽ broadcast event `SCREEN_SHARE_REQUESTED` tới **toàn bộ phòng** (Host sẽ nhận).

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

Backend sẽ:
1. Cập nhật Participant Metadata của `requesterId` với `canShareScreenOverride: true`
2. Gửi event `SCREEN_SHARE_APPROVED` riêng tới participant được duyệt

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

Backend gửi event `SCREEN_SHARE_REJECTED` riêng tới participant bị từ chối.

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

Backend sẽ:
1. Server-side mute track `SCREEN_SHARE` của `targetId` trên LiveKit
2. Thu hồi `canShareScreenOverride` trong Participant Metadata
3. Broadcast event `SCREEN_SHARE_STOPPED` tới toàn bộ phòng

---

## 3. Nhận Trạng Thái Realtime (WebSocket + LiveKit)

### 3a. Topic STOMP cần subscribe

| Topic | Ai subscribe | Mục đích |
|-------|-------------|---------|
| `/topic/meeting/{meetingCode}` | Tất cả | Nhận setting thay đổi, yêu cầu share, share bị dừng |
| `/topic/meeting/{meetingCode}/participant/{participantId}` | Chính người đó | Nhận kết quả approve/reject |

---

### 3b. Bảng tất cả WebSocket event liên quan

#### `ROOM_SETTINGS_CHANGED`
Gửi đến: `/topic/meeting/{meetingCode}` (toàn phòng)

```json
{
    "meetingCode": "abc12",
    "action": "ROOM_SETTINGS_CHANGED",
    "extra": {
        "allowParticipantShareScreen": false
    }
}
```

→ Frontend cập nhật trạng thái phòng, điều chỉnh luồng xử lý nút share màn hình.

---

#### `SCREEN_SHARE_REQUESTED`
Gửi đến: `/topic/meeting/{meetingCode}` (toàn phòng — Host sẽ nhận)

```json
{
    "meetingCode": "abc12",
    "action": "SCREEN_SHARE_REQUESTED",
    "targetParticipantId": 42,
    "targetName": "Nguyen Van A"
}
```

→ Host hiển thị popup/toast: _"Nguyen Van A muốn chia sẻ màn hình"_ kèm nút Duyệt / Từ chối.

---

#### `SCREEN_SHARE_APPROVED`
Gửi đến: `/topic/meeting/{meetingCode}/participant/{requesterId}` (chỉ người được duyệt)

```json
{
    "meetingCode": "abc12",
    "action": "SCREEN_SHARE_APPROVED",
    "targetParticipantId": 42,
    "targetName": null
}
```

→ Frontend của participant đó **tự động bắt đầu** luồng share màn hình (gọi LiveKit `localParticipant.setScreenShareEnabled(true)`).

---

#### `SCREEN_SHARE_REJECTED`
Gửi đến: `/topic/meeting/{meetingCode}/participant/{requesterId}` (chỉ người bị từ chối)

```json
{
    "meetingCode": "abc12",
    "action": "SCREEN_SHARE_REJECTED",
    "targetParticipantId": 42,
    "targetName": null
}
```

→ Hiển thị thông báo: _"Yêu cầu chia sẻ màn hình đã bị từ chối."_

---

#### `SCREEN_SHARE_STOPPED`
Gửi đến: `/topic/meeting/{meetingCode}` (toàn phòng)

```json
{
    "meetingCode": "abc12",
    "action": "SCREEN_SHARE_STOPPED",
    "targetParticipantId": 42,
    "targetName": null
}
```

→ Frontend cập nhật UI: xóa luồng màn hình của `targetParticipantId` khỏi giao diện. Client của `targetParticipantId` nhận thêm `TrackMuted` từ LiveKit và tắt UI share màn hình.

---

### 3c. LiveKit Room Metadata

Khi Host gọi settings API, LiveKit tự động broadcast `RoomMetadataChanged` xuống tất cả client:

```typescript
room.on(RoomEvent.RoomMetadataChanged, (metadata: string) => {
    const incoming = JSON.parse(metadata);
    // merge để không mất các key khác
    roomSettings = { ...roomSettings, ...incoming };

    if (incoming.allowParticipantShareScreen !== undefined) {
        handleScreenShareSettingChanged(incoming.allowParticipantShareScreen);
    }
});
```

**Khi người mới join** — đọc ngay sau `room.connect()`:
```typescript
room.on(RoomEvent.Connected, () => {
    if (room.metadata) {
        const settings = JSON.parse(room.metadata);
        roomSettings = settings;
    }
});
```

---

### 3d. LiveKit Participant Metadata (canShareScreenOverride)

Khi Host approve, backend cập nhật metadata của participant được duyệt:
```json
{
    "participantId": 42,
    "role": "PARTICIPANT",
    "canShareScreenOverride": true
}
```

Frontend có thể đọc metadata này qua LiveKit event để kiểm tra quyền:
```typescript
room.on(RoomEvent.ParticipantMetadataChanged, (metadata, participant) => {
    if (participant.isLocal) {
        const meta = JSON.parse(metadata ?? '{}');
        if (meta.canShareScreenOverride === true) {
            // Đã được approve - bắt đầu share
            startScreenShare();
        }
    }
});
```

> Tuy nhiên, cách đơn giản hơn là lắng nghe STOMP event `SCREEN_SHARE_APPROVED` ở mục 3b.

---

## 4. Logic UI Cần Xử Lý

### 4a. Nút Share Màn Hình phía Participant

```
allowParticipantShareScreen = true  → Bấm nút → Share ngay (LiveKit trực tiếp)
allowParticipantShareScreen = false → Bấm nút → Gọi API xin phép → Chờ kết quả
```

```typescript
async function onScreenShareButtonClick() {
    if (roomSettings.allowParticipantShareScreen) {
        // Chế độ tự do: share thẳng qua LiveKit
        await localParticipant.setScreenShareEnabled(true);
    } else {
        // Chế độ khoá: gửi yêu cầu lên backend
        await requestScreenShare();
        showToast('Đã gửi yêu cầu chia sẻ màn hình, chờ Host duyệt...');
    }
}
```

### 4b. Popup phía Host khi nhận `SCREEN_SHARE_REQUESTED`

```typescript
stompClient.subscribe(`/topic/meeting/${meetingCode}`, (frame) => {
    const msg = JSON.parse(frame.body);
    if (msg.action === 'SCREEN_SHARE_REQUESTED') {
        showApprovalDialog({
            participantId: msg.targetParticipantId,
            name: msg.targetName,
            onApprove: () => approveScreenShare(msg.targetParticipantId),
            onReject:  () => rejectScreenShare(msg.targetParticipantId),
        });
    }
});
```

### 4c. Giao diện toggle phía Host

```typescript
async function onToggleScreenShareSetting(allowFree: boolean) {
    await fetch(`/api/meetings/${meetingCode}/settings`, {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
            'Meeting-Token': meetingToken,
        },
        body: JSON.stringify({ allowParticipantShareScreen: allowFree }),
    });
}
```

### 4d. Nút Force Stop phía Host (hiển thị khi có người đang share)

```typescript
async function onForceStopScreenShare(targetParticipantId: number) {
    await fetch(`/api/meetings/${meetingCode}/screen-share/${targetParticipantId}/stop`, {
        method: 'POST',
        headers: { 'Meeting-Token': meetingToken },
    });
}
```

---

## 5. Tóm Tắt Luồng Theo Vai Trò

### Host
```
Bật/tắt setting → PATCH /settings
Nhận SCREEN_SHARE_REQUESTED → hiển thị popup → POST .../approve | .../reject
Muốn dừng ai đó → POST .../screen-share/{targetId}/stop
```

### Participant (khi phòng khoá)
```
Bấm share → POST .../screen-share-requests
Chờ...
├── Nhận SCREEN_SHARE_APPROVED → tự động share (setScreenShareEnabled)
└── Nhận SCREEN_SHARE_REJECTED → hiển thị thông báo
```

### Tất cả (qua LiveKit / STOMP)
```
RoomMetadataChanged       → cập nhật roomSettings
ROOM_SETTINGS_CHANGED     → cập nhật roomSettings (backup)
SCREEN_SHARE_STOPPED      → ẩn/xóa luồng màn hình của targetParticipantId
TrackMuted (LiveKit)      → cập nhật UI track bị tắt
```
