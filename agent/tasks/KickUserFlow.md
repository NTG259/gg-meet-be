# Luồng xử lý Kích (Kick) User Khỏi Phòng Họp

Tài liệu này mô tả chi tiết luồng nghiệp vụ khi một người dùng (Host) kích một người dùng khác ra khỏi phòng họp, dựa trên phân tích cấu trúc mã nguồn Backend của dự án.

## 1. Môi trường và Lớp liên quan

- **Protocol:** WebSocket
- **Controller:** `MeetingWebSocketController.java`
- **Service Orchestrator:** `MeetingOrchestratorService.java`
- **Service Component:** `ParticipantServiceRF`, `ParticipantEntityService`, `NotificationService`, `ParticipantPermissionService`
- **Model / Entity:** `MeetingWsMessage`, `MeetingParticipant`
- **Enum Trạng thái:** `ParticipantStatus.BLOCK` (có thể được áp dụng khi user bị cấm/kick)

---

## 2. Chi tiết luồng (Flow)

Dưới đây là các bước từ lúc người dùng thao tác trên giao diện tới khi xử lý xong phía Backend:

### Bước 1: Client gửi yêu cầu kích user
1. Trên giao diện người dùng (UI), người có quyền Host chọn một user cụ thể và bấm "Kick/Đuổi".
2. Client (React/Vue/...) gửi một bản tin (message) qua kênh WebSocket.
   - **Endpoint (Destination):** `/app/meeting/kickout`
   - **Payload (`MeetingWsMessage`):** 
     - `meetingCode`: Mã phòng họp.
     - `targetParticipantId`: ID của người dùng sẽ bị đuổi.

### Bước 2: Controller tiếp nhận
1. Hàm `kickout` trong `MeetingWebSocketController` sử dụng annotation `@MessageMapping("/meeting/kickout")` để bắt các yêu cầu gửi đến.
2. Từ `SimpMessageHeaderAccessor` (lưu trữ thông tin session kết nối WebSocket), hệ thống trích xuất ra `hostParticipantId` (ID của Host đang thao tác, đã được lưu trong quá trình xác thực kết nối ban đầu).
3. Controller truyền các tham số `meetingCode`, `targetParticipantId`, `hostParticipantId` xuống cho service layer: `meetingOrchestratorService.kickout(...)`.

### Bước 3: Xác thực và xử lý tại Orchestrator Service
Tại hàm `kickout` của `MeetingOrchestratorService`, trình tự xử lý được thực thi như sau:

1. **Xác thực quyền hạn (Permission Control):**
   - Lấy thông tin Participant của Host dựa trên `hostId` và `meetingCode`.
   - Gọi `permissionService.assertHostPermission(host)` để kiểm tra quyền. Nếu người dùng không phải là `HOST` thì ứng dụng sẽ ném ra lỗi (Exception) và quá trình bị huỷ bỏ.

2. **Cấm/Kích người dùng (Ban/Kick User):**
   - Gọi `participantService.banUser(targetParticipantId, meetingCode)`. Hàm này sẽ xử lý logic đổi trạng thái người dùng (ví dụ có thể cập nhật trạng thái `ParticipantStatus` thành `BLOCK` hoặc xoá bỏ khỏi cơ sở dữ liệu để ngăn họ vào lại, và yêu cầu tắt kết nối media trên LiveKit nếu cần thiết).
   *(Lưu ý: Logic bên trong hàm `banUser` sẽ chịu trách nhiệm chính về mặt cập nhật DB và tương tác LiveKit nếu có).*

3. **Lấy thông tin người bị kích:**
   - Query xuống DB thông qua `participantEntityService.getParticipantInMeeting(...)` để lấy chính xác thông tin (entity `MeetingParticipant`) của người bị đuổi (tên, id,...).

4. **Gửi thông báo (Notification):**
   - `notificationService.sendUserKicked(meetingCode, kicked.getParticipantId(), kicked.getGuestName())` được gọi.
   - Thao tác này sẽ gửi ra một thông điệp WebSocket (broadcast hoặc unicast) xuống Client để báo hiệu người dùng này đã bị kick.

### Bước 4: Phản hồi về Client
1. Nhận được thông báo sự kiện (event) Kicked từ hệ thống `NotificationService` qua WebSocket:
   - **Với User bị kích:** Ứng dụng Client có thể hiển thị cảnh báo (VD: *"Bạn đã bị mời ra khỏi phòng"*) và tự động ngắt kết nối LiveKit, điều hướng (redirect) về trang chủ/trang chờ.
   - **Với các User còn lại trong phòng:** Nhận tín hiệu để tiến hành cập nhật lại giao diện, gỡ thẻ video, và làm mới danh sách người tham gia đang hiển thị.

---

## 3. Luồng Kích Người Dùng Mềm (Soft Kick - Không Block)

Đây là luồng bổ sung dành cho trường hợp Host chỉ muốn mời người dùng ra khỏi phòng tại thời điểm hiện tại (người dùng vẫn có thể xin vào lại sau nếu cần) thay vì cấm vĩnh viễn (Block).

### Điểm khác biệt so với luồng Kick/Ban ở trên:

1. **Gửi yêu cầu:**
   - Client gửi yêu cầu với một Endpoint có parameter `isBan = false`

2. **Xử lý tại Service:**
   - Thay vì gọi `banUser`, hệ thống sẽ gọi phương thức cập nhật trạng thái người dùng về `LEFT`: `participantEntityService.updateParticipantStatus(participant, ParticipantStatus.LEFT)`.

3. **Thu hồi Token (Ngắt kết nối LiveKit):**
   - Hệ thống sẽ gọi API của LiveKit Server thông qua `LiveKitService` để xoá người dùng khỏi phòng media ngay lập tức: 
     `roomServiceClient.removeParticipant(meetingCode, identity)`
   - *Lưu ý:* `identity` ở đây thường là `user-{id}` hoặc `guest-{id}`.

4. **Gửi thông báo:**
   - Hệ thống vẫn gửi thông báo WebSocket (tương tự như `notificationService.sendUserKicked`) để báo cho Frontend biết người dùng đã bị đưa ra khỏi phòng nhằm làm mới UI.

---
