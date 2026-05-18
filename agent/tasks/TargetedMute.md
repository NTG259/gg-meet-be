# Tài Liệu Chức Năng: Tắt Mic / Camera Từng Người Dùng (Soft Mute)

## 1. Tổng Quan

Host có quyền chọn một người tham gia cụ thể trong phòng và thực hiện thao tác **tắt Mic** hoặc **tắt Camera** của người đó.

**Đặc điểm quan trọng (Soft Mute):**
- Hành động này **không khóa** quyền sử dụng thiết bị của người dùng.
- Người bị tắt Mic/Camera hoàn toàn có thể tự bấm nút "Bật Mic" hoặc "Bật Camera" để mở lại ngay lập tức nếu muốn.
- Đây đơn thuần là thao tác Host ép dừng luồng (track) âm thanh hoặc hình ảnh hiện tại của người dùng.

---

## 2. API

### Yêu cầu tắt Mic / Camera của một người dùng

```
POST /api/meetings/{meetingCode}/participants/{targetId}/mute
```

**Headers:**
```
Meeting-Token: <meeting_token>
Content-Type: application/json
```

**Request Body:**
```json
{
    "trackType": "AUDIO" // Hoặc "VIDEO"
}
```

**Response (200 OK):**
```json
{
    "data": null,
    "message": "Participant track muted successfully",
    "error": null,
    "statusCode": 200
}
```

**Lỗi có thể gặp:**
| HTTP Status | Ý nghĩa |
|-------------|---------|
| 401 | Meeting-Token không hợp lệ |
| 403 | Người gọi không phải HOST |
| 404 | Không tìm thấy `targetId` trong phòng |
| 400 | Người dùng `targetId` hiện đang không bật Mic/Camera |

---

## 3. Hành vi hệ thống phía Server

1. **Xác thực quyền**: Backend kiểm tra token của người gọi (Host).
2. **Tìm track tương ứng trên LiveKit**: 
   - Backend sử dụng LiveKit SDK (`roomServiceClient.getParticipant(roomName, identity)`) để lấy thông tin của `targetId`.
   - Tìm kiếm các Track đang được publish bởi người này.
   - Nếu `trackType == "AUDIO"`, tìm track có `source == TrackSource.MICROPHONE`.
   - Nếu `trackType == "VIDEO"`, tìm track có `source == TrackSource.CAMERA`.
3. **Thực thi tắt (Server-side Mute)**:
   - Backend gọi API của LiveKit: `roomServiceClient.mutePublishedTrack(roomName, identity, trackSid, true)`.
   - LiveKit Server sẽ ngắt luồng dữ liệu của Track này.
4. **Broadcast Event (Tự động bởi LiveKit)**:
   - Khi Backend gọi hàm trên thành công, LiveKit Server sẽ tự động phát sinh sự kiện `TrackMuted` gửi xuống các Client trong phòng.
   - Các Client khác sẽ thấy biểu tượng Mic/Cam của `targetId` chuyển sang trạng thái tắt.
   - Client của chính `targetId` sẽ nhận được thông báo từ LiveKit SDK và tự động cập nhật UI (chuyển nút Mic/Cam sang trạng thái tắt). Vì Backend **không** thay đổi Room Metadata (như tính năng khóa mic toàn phòng), nên nút bấm của người dùng không bị `disabled` và họ có thể bấm vào để bật lại.

---

## 4. Các sự kiện Realtime (WebSocket + LiveKit)

- Tính năng này **không cần** Backend phải gửi thêm event thông qua STOMP WebSocket. Toàn bộ quá trình đồng bộ trạng thái Track (đang bật hay bị tắt) được **LiveKit SDK xử lý hoàn toàn tự động** thông qua các event có sẵn như `TrackMuted` và `TrackUnmuted`. 
- Frontend chỉ cần bắt các sự kiện mặc định của LiveKit để vẽ lại giao diện.
