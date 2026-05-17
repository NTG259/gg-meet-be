# Kịch Bản Kỹ Thuật: Chức Năng Tắt Mic Toàn Bộ & Chỉ Host Được Mở Mic

Tài liệu này mô tả luồng xử lý (workflow) và kiến trúc kỹ thuật để thực hiện tính năng **"Cập nhật cài đặt phòng họp: Tắt mic toàn bộ và chặn người tham gia tự mở mic (chỉ Host mới có quyền mở)"** trong hệ thống sử dụng Spring Boot và LiveKit.

---

## 1. Yêu cầu hệ thống (Requirements)
- **Host** có quyền thay đổi trạng thái cài đặt phòng: Bật/tắt chế độ `allowParticipantUnmute` (Cho phép người tham gia tự mở mic).
- Khi Host chọn **"Tắt mic toàn bộ & Không cho phép tự mở"**:
  1. Tất cả người tham gia (trừ Host) ngay lập tức bị tắt mic (Hard Mute).
  2. Nút "Bật Mic" (Unmute) trên giao diện của người tham gia (trừ Host) sẽ bị vô hiệu hóa (disabled).
- Khi Host tắt chế độ này (cho phép mở lại):
  1. Nút "Bật Mic" của người tham gia khả dụng trở lại.
  2. Người tham gia vẫn đang bị tắt mic, nhưng họ có thể tự bấm mở lại nếu muốn.

---

## 2. Thiết kế dữ liệu (Data & State Management)

### a. Room Metadata (LiveKit)
Thay vì chỉ lưu cài đặt trong Database, việc sử dụng **Room Metadata** của LiveKit là tối ưu nhất vì nó đồng bộ realtime (thời gian thực) xuống tất cả các client đang kết nối mà không cần tự build WebSockets riêng.
- Metadata của phòng (Room Metadata) sẽ chứa một JSON object.
- Thêm trường: `"allowParticipantUnmute": boolean` (Mặc định: `true`).

### b. Database (Tuỳ chọn)
- Nếu cần lưu lại trạng thái này khi cuộc họp bị sập và tạo lại, có thể thêm một trường `allow_participant_unmute` (boolean) vào bảng `meetings`.

---

## 3. Luồng xử lý chi tiết (Workflow)

### Bước 1: Host gửi yêu cầu thay đổi cài đặt (API Request)
Host thao tác trên giao diện, Client gọi API lên Backend:
- **Endpoint:** `PATCH /api/meetings/{meetingCode}/settings`
- **Body:**
  ```json
  {
      "allowParticipantUnmute": false
  }
  ```
- **Xác thực:** Backend (trong `MeetingOrchestratorService`) kiểm tra `actorParticipantId` có `role == HOST` hay không. Nếu không, ném lỗi `Forbidden`.

### Bước 2: Backend xử lý cập nhật LiveKit
Trong `LiveKitService`, thêm hàm cập nhật Room Metadata và buộc tắt mic các track hiện tại.

**1. Cập nhật Room Metadata:**
Backend sử dụng `RoomServiceClient` của LiveKit để update metadata:
```java
String newMetadata = objectMapper.writeValueAsString(Map.of("allowParticipantUnmute", false));
roomServiceClient.updateRoomMetadata(meetingCode, newMetadata).execute();
```
*(Ngay khi gọi hàm này, LiveKit Server sẽ tự động gửi event `roomMetadataChanged` xuống toàn bộ Client).*

**2. Ép tắt mic hiện tại của các Participant (Server-side Mute):**
Nếu `allowParticipantUnmute` đổi từ `true` sang `false`, Backend gọi LiveKit Server để mute tất cả các Audio Track đang publish của những người không phải là Host.
- Lấy danh sách participants từ LiveKit: `roomServiceClient.listParticipants(meetingCode)`
- Lọc ra những participant có role không phải HOST (dựa vào identity hoặc metadata của participant).
- Với mỗi Audio Track đang publish, gọi API tắt track:
  ```java
  roomServiceClient.mutePublishedTrack(
      meetingCode,
      participant.getIdentity(),
      audioTrack.getSid(),
      true // muted = true
  ).execute();
  ```

### Bước 3: Broadcast trạng thái cập nhật (Backend)
Backend không cần quan tâm Frontend xử lý UI ra sao, chỉ chịu trách nhiệm đảm bảo tất cả client trong phòng đều nhận được tín hiệu "Đã thay đổi trạng thái mic".

**1. Broadcast thông qua Room Metadata của LiveKit:**
Khi `roomServiceClient.updateRoomMetadata` được gọi thành công, LiveKit Server tự động broadcast event `RoomMetadataChanged` kèm chuỗi JSON mới xuống tất cả client trong phòng. Các client tự parse chuỗi này để biết `allowParticipantUnmute` đang là true hay false.

**2. Broadcast thông qua NotificationService (WebSocket nội bộ - Tuỳ chọn nếu cần):**
Nếu hệ thống có sẵn WebSockets quản lý meeting (qua `NotificationService`), Backend có thể gọi thêm một hàm để gửi tín hiệu riêng:
```java
notificationService.sendMeetingSettingsUpdated(meetingCode, allowParticipantUnmute);
```
Tín hiệu này giúp client nhận biết thiết lập phòng đã thay đổi để tắt giao diện nút Mic nếu cần.

---

## 4. Xử lý trường hợp người dùng mới tham gia (Late Joiners)

Khi một người dùng mới vào phòng, backend tạo LiveKit Token (trong `LiveKitService.buildLiveKitToken`).
- Nếu sử dụng DB để lưu `allowParticipantUnmute`, Backend có thể đọc DB để quyết định.
- Tuy nhiên, vì Room Metadata của LiveKit luôn được lưu giữ cho phòng đó, nên khi Client mới kết nối vào phòng (`room.connect`), Client sẽ tự động nhận được Room Metadata hiện tại ngay lập tức. Client chỉ cần dựa vào Room Metadata này để vô hiệu hóa nút Mic (không cần Backend phải nhúng `allowParticipantUnmute` vào JWT Token của user).

---

## 5. Khóa tính năng xin phép mở mic
Một khi chế độ `allowParticipantUnmute` được gán thành `false`:
- Giao diện của Client (người tham gia) không những khóa nút bật mic mà còn **không hiển thị hoặc vô hiệu hóa nút "Yêu cầu mở mic" (Request to Unmute)**.
- Backend không cần cung cấp API để người tham gia gửi yêu cầu mở mic tới Host trong trạng thái này (nếu có API đó, backend sẽ chặn lại và trả về lỗi nếu `allowParticipantUnmute` đang là `false`).
- Host vẫn có thể chủ động mời một người mở mic (nếu hệ thống hỗ trợ tính năng "Host yêu cầu user mở mic" - Host to User), nhưng luồng giao tiếp User to Host để xin mở mic hoàn toàn bị cấm.

---

## 6. Tổng kết các API cần bổ sung
1. **API**: `PATCH /api/meetings/{meetingCode}/settings`
2. **Service RF**: Cập nhật hàm trong `MeetingOrchestratorService` để handle API trên.
3. **LiveKitService**:
   - `updateRoomMetadata(String meetingCode, String metadata)`
   - `forceMuteAllNonHosts(String meetingCode)` (Dùng LiveKit SDK list participants & mute tracks).
