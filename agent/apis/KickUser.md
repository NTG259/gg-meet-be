# API Docs: Đuổi người dùng (Kick User)

## 1. Gửi Yêu Cầu Đuổi Người Dùng (Client -> Server)

Chức năng đuổi người dùng (và có thể cấm quay lại) được thực hiện thông qua WebSocket (STOMP). Chỉ Host (Chủ phòng) mới có quyền thực hiện hành động này.

**STOMP Destination:**
```
SEND /app/meeting/kickout
```

**Payload (JSON):**
```json
{
    "meetingCode": "string",
    "targetParticipantId": 123,
    "isBan": true 
}
```

> **Giải thích tham số:**
> - `meetingCode`: Mã phòng họp hiện tại.
> - `targetParticipantId`: ID (Participant ID) của người tham gia cần bị đuổi.
> - `isBan`: `true` nếu muốn cấm người này vào lại phòng (Status sẽ thành `BLOCK`), `false` nếu chỉ đuổi tạm thời, có thể xin vào lại (Status sẽ thành `LEFT`). Mặc định trên server là `true` nếu client không truyền.

## 2. Xử Lý Phía Server

Khi nhận được thông điệp, hệ thống (Backend + LiveKit) xử lý qua các bước:
1. Xác thực người gửi có phải là Host hay không.
2. Kiểm tra xem người bị đuổi có tồn tại trong phòng hay không.
3. Cập nhật trạng thái người dùng trong CSDL sang `BLOCK` hoặc `LEFT` tùy vào tham số `isBan`.
4. Gọi API qua LiveKit (`liveKitService.removeParticipant`) để ngắt kết nối WebSocket/WebRTC của người dùng bị đuổi ra khỏi LiveKit Room ngay lập tức.
5. Broadcast sự kiện STOMP để thông báo đuổi thành công đến các clients.

## 3. Lắng Nghe Sự Kiện Trả Về (Server -> Client)

Sau khi xử lý thành công, Backend tự động gửi thông điệp cho người bị đuổi qua kênh riêng và cho toàn bộ người tham gia ở kênh chung nhằm cập nhật UI tương ứng.

### 3.1 Thông báo riêng cho người bị đuổi

**STOMP Topic:**
```
SUBSCRIBE /topic/meeting/{meetingCode}/participant/{targetParticipantId}
```

**Event Payload (JSON):**
```json
{
    "action": "USER_KICKED",
    "meetingCode": "string",
    "targetParticipantId": 123,
    "targetName": "Tên người bị đuổi"
}
```

**Hành vi Client (Người bị đuổi):** Khi nhận được sự kiện này qua STOMP, frontend cần hiển thị Modal hoặc Toast thông báo (VD: "Bạn đã bị chủ phòng mời ra khỏi cuộc họp") và tự động điều hướng người dùng rời khỏi phòng, quay lại màn hình Home.

### 3.2 Thông báo chung cho cả phòng

**STOMP Topic:**
```
SUBSCRIBE /topic/meeting/{meetingCode}
```

**Event Payload (JSON):**
```json
{
    "action": "USER_KICKED",
    "meetingCode": "string",
    "targetParticipantId": 123,
    "targetName": "Tên người bị đuổi"
}
```

**Hành vi Client (Toàn bộ phòng):**
- Khi nhận sự kiện từ STOMP, frontend cập nhật lại danh sách người tham gia (loại bỏ `targetParticipantId` ra khỏi danh sách đang hiển thị).
- **Lưu ý phụ:** Ngoài STOMP, SDK của LiveKit cũng tự động phát sự kiện `ParticipantDisconnected` khi LiveKit server ngắt user này. Frontend có thể sử dụng kết hợp STOMP và SDK của LiveKit để đảm bảo đồng bộ giao diện lưới camera và danh sách lưới (Grid).

## 4. Xử Lý Ngoại Lệ

- Nếu người gửi không phải là Host: Ném ra lỗi `FORBIDDEN` (không có quyền), server không xử lý yêu cầu.
- Nếu `targetParticipantId` không hợp lệ hoặc không có mặt trong phòng: Ném ra lỗi `PARTICIPANT_NOT_FOUND` hoặc lỗi trạng thái.
