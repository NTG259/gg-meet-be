# Kịch Bản Kỹ Thuật: Chức Năng Quản Lý Chia Sẻ Màn Hình (Host Only & Request to Share)

Tài liệu này mô tả luồng xử lý (workflow) và kiến trúc kỹ thuật để thực hiện tính năng **"Quản lý chia sẻ màn hình"**, trong đó mặc định chỉ Host được chia sẻ, Participant phải xin phép và Host có quyền ngắt chia sẻ màn hình của bất kỳ ai đang chia sẻ.

---

## 1. Yêu cầu hệ thống (Requirements)
- **Mặc định:** Bất kỳ ai tham gia phòng (Host và Participant) đều có quyền tự do chia sẻ màn hình.
- **Cài đặt phòng (Settings):** Host có quyền thay đổi cài đặt phòng để chặn người tham gia tự ý chia sẻ màn hình.
- **Xin phép (Request):** Chỉ khi Host đã khoá tính năng chia sẻ tự do, Participant muốn chia sẻ mới phải gửi yêu cầu (Request) để xin phép Host.
- **Phê duyệt (Approve/Reject):** Host đồng ý, người tham gia mới bắt đầu được chia sẻ màn hình. Host từ chối, gửi thông báo báo về.
- **Ép tắt (Force Stop):** Dù phòng đang ở chế độ tự do hay khoá, Host luôn có quyền ép buộc dừng luồng chia sẻ màn hình của bất kỳ ai ngay lập tức.

---

## 2. Thiết kế dữ liệu và Quản lý trạng thái

### a. Room Metadata (Cài đặt phòng chung)
- Sử dụng Room Metadata của LiveKit để lưu trạng thái cài đặt chia sẻ màn hình (tương tự như tắt mic).
- Trường: `"allowParticipantShareScreen": true` (Mặc định là true - ai cũng có thể share).

### b. Participant Metadata (Cấp quyền ngoại lệ)
- Khi `"allowParticipantShareScreen"` bị Host set thành `false`, hệ thống sẽ yêu cầu "ai được cấp phép mới được share". Ta cần lưu trạng thái cấp quyền này vào Metadata của từng Participant trong LiveKit.
- Khi Host duyệt, ta update Metadata của người đó: `"canShareScreenOverride": true`.

---

## 3. Luồng xử lý chi tiết (Workflow)

### Luồng 0: Host thay đổi cài đặt chia sẻ màn hình
1. **API Request (Host gửi):**
   - Host thao tác tắt/bật quyền chia sẻ tự do, Client gọi API:
   - **Endpoint:** `PATCH /api/meetings/{meetingCode}/settings`
   - **Body:** `{"allowParticipantShareScreen": false}`
2. **Backend xử lý & Broadcast:**
   - Backend xác thực Host và update **Room Metadata** qua LiveKit Server SDK.
   - LiveKit tự động broadcast `RoomMetadataChanged` xuống tất cả Client.
   - **Lưu ý UI:** Ở phía Frontend, nút Share Screen của Participant **vẫn luôn sáng (không bị disable)**. Tuy nhiên, nếu setting là `false` và họ click vào nút đó, thay vì bắt đầu chia sẻ ngay, hệ thống sẽ hiện thông báo chặn lại hoặc popup gọi API xin phép Host.
   - Nếu có Participant nào đang share màn hình mà Host đổi setting sang `false`, Backend có thể quét và gọi **Force Stop** luôn các luồng màn hình đó để đảm bảo tuân thủ rules mới.

### Luồng 1: Người tham gia xin phép chia sẻ màn hình (Khi phòng bị khoá)
1. **API Request (Participant gửi):**
   - **Endpoint:** `POST /api/meetings/{meetingCode}/screen-share-requests`
2. **Backend xử lý & Broadcast:**
   - Kiểm tra user có tồn tại trong phòng không.
   - Sử dụng `NotificationService` để broadcast thông báo xin phép lên cho Host.
   - **Mã giả Backend:**
     ```java
     notificationService.sendScreenShareRequested(meetingCode, requesterId, requesterName);
     ```

### Luồng 2: Host phê duyệt yêu cầu (Approve)
1. **API Request (Host gửi):**
   - **Endpoint:** `POST /api/meetings/{meetingCode}/screen-share-requests/{requesterId}/approve`
2. **Backend xử lý & Broadcast:**
   - Xác thực quyền Host của người thực hiện thao tác.
   - Update Participant Metadata qua LiveKit Server SDK để đánh dấu user này có quyền share tạm thời.
   - Broadcast thông báo cho user đó (để Frontend của user đó nhận biết yêu cầu đã được duyệt và có thể tự động bắt đầu quá trình chia sẻ màn hình).
   - **Mã giả Backend:**
     ```java
     1. (Tùy chọn) Lưu metadata ngoại lệ vào LiveKit cho requester
     roomServiceClient.updateParticipant(meetingCode, requesterIdentity, metadata, permissions);
     
     2. Broadcast thông báo phê duyệt
     notificationService.sendScreenShareApproved(meetingCode, requesterId);
     ```

*(Luồng Reject tương tự nhưng gọi `notificationService.sendScreenShareRejected` và không cấp quyền).*

### Luồng 3: Host ép tắt chia sẻ màn hình của Participant (Force Stop)
1. **API Request (Host gửi):**
   - **Endpoint:** `POST /api/meetings/{meetingCode}/screen-share/{targetId}/stop`
2. **Backend xử lý & Broadcast:**
   - Xác thực quyền Host.
   - Backend gọi API lên LiveKit Server lấy danh sách các Track (luồng video/audio) của user bị ngắt.
   - Tìm kiếm Track có liên quan đến việc chia sẻ màn hình (trong LiveKit, source của track share màn hình thường là `SCREEN_SHARE`).
   - Thực hiện lệnh **Mute (tắt) Track** đó trực tiếp từ phía Server.
   - Thu hồi lại quyền ngoại lệ trong Participant Metadata của user đó.
   - **Mã giả Backend:**
     ```java
     1. Tắt Track màn hình của user đó
     roomServiceClient.mutePublishedTrack(
         meetingCode,
         targetIdentity,
         screenShareTrackSid,
         true // muted = true
     ).execute();
     
     2. Broadcast sự kiện để phòng biết màn hình đã bị tắt
     notificationService.sendScreenShareStopped(meetingCode, targetId);
     ```

---

## 4. Tổng kết các API cần bổ sung (Backend)

1. **`PATCH /api/meetings/{meetingCode}/settings`**
   - **Role:** Host.
   - **Mục đích:** Bật/tắt quyền chia sẻ màn hình tự do của phòng (cập nhật Room Metadata).

2. **`POST /api/meetings/{meetingCode}/screen-share-requests`**
   - **Role:** Participant.
   - **Mục đích:** Bắn tín hiệu xin phép chia sẻ màn hình lên Host (khi phòng đang khoá).

3. **`POST /api/meetings/{meetingCode}/screen-share-requests/{requesterId}/approve`**
   - **Role:** Host.
   - **Mục đích:** Duyệt yêu cầu chia sẻ màn hình, cấp quyền ngoại lệ và broadcast tín hiệu cho phép.

4. **`POST /api/meetings/{meetingCode}/screen-share-requests/{requesterId}/reject`** (Tuỳ chọn)
   - **Role:** Host.
   - **Mục đích:** Từ chối yêu cầu và gửi broadcast thông báo lại cho Participant.

5. **`POST /api/meetings/{meetingCode}/screen-share/{targetId}/stop`**
   - **Role:** Host.
   - **Mục đích:** Ép tắt luồng chia sẻ màn hình của một Participant ngay lập tức bằng LiveKit Server SDK.
