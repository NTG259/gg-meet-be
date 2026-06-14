    # Plan: Socket Flow Bug Analysis & Fix
    
    ---
    
    ## 1. Cơ Chế Hoạt Động Hiện Tại
    
    ### 1.1 Kiến trúc WebSocket
    
    Project sử dụng **Spring STOMP over SockJS** với **SimpleBroker** (in-memory, không persist message).
    
    ```
    Client                    Server (Spring)
      |                            |
      |-- CONNECT (Meeting-Token) →|  WebSocketConfig.authenticateWebSocketSession()
      |                            |  → validate JWT, lưu {participantId, meetingCode, role} vào session
      |
      |-- SUBSCRIBE /topic/... →   |  WebSocketConfig.authorizeSubscription()
      |                            |  → kiểm tra role/participantId phù hợp với destination
    ```
    
    **Các topic:**
    
    | Topic | Ai nhận | Mô tả |
    |---|---|---|
    | `/topic/meeting/{code}` | Tất cả participant | Broadcast chung |
    | `/topic/meeting/{code}/waiting` | Chỉ HOST | Thông báo waiting room |
    | `/topic/meeting/{code}/participant/{id}` | Chỉ participant đó | Thông báo riêng tư |
    
    ### 1.2 Luồng Join Meeting
    
    ```
    User A                   Server                          Host
      |                        |                               |
      |-- POST /join -------->  |                              |
      |                        |-- handleAuthenticatedJoin()   |
      |                        |   findExistingParticipant()   |
      |                        |   handleParticipantStatus()   |
      |                        |   → tạo/cập nhật participant  |
      |                        |   → status = WAITING          |
      |                        |                               |
      |                        |-- sendJoinRequest()           |
      |                        |   → /topic/.../waiting -----> |  [JOIN_REQUEST]
      |                        |                               |
      |<-- response (WAITING) --|                              |
      |   (meetingToken)        |                              |
    ```
    
    ### 1.3 Luồng Host Reject
    
    ```
    Host                     Server                          User A
      |                        |                               |
      |-- WS: /reject -------> |                               |
      |   (targetParticipantId)|-- handleRejectParticipant()   |
      |                        |   status WAITING → REJECT     |
      |                        |   save DB                     |
      |                        |                               |
      |                        |-- sendReject()                |
      |                        |   → /participant/{userA} ---> |  [REJECTED]
      |<-- /participant/{host} -|  [REJECT_SUCCESS]            |
    ```
    
    ### 1.4 Luồng Re-request (kịch bản lỗi)
    
    ```
    User A                   Server                          Host
      |                        |                               |
      |  (nhận REJECTED)       |                               |
      |-- POST /join -------->  |                              |
      |                        |-- handleAuthenticatedJoin()   |
      |                        |   findExistingParticipant()   |
      |                        |   handleParticipantStatus()   |
      |                        |   REJECT → WAITING, save      |
      |                        |                               |
      |                        |-- sendJoinRequest()  ← (A)    |
      |                        |   → /topic/.../waiting -----> |  [JOIN_REQUEST]
      |                        |                               |  ???
      |<-- response (WAITING) --|  ← (B) transaction commit    |
    ```
    
    ---
    
    ## 2. Mô Tả Lỗi Ẩn
    
    ### Bug #1 — Notification gửi trước khi transaction commit (Root Cause)
    
    **File:** `MeetingOrchestratorService.java` — `joinRoomByUser()` (line 111–146) và `joinRoomByGuest()` (line 81–109)
    
    **Cơ chế gây lỗi:**
    
    ```java
    @Transactional   // transaction MỞ ở đây
    public MeetingResponseDTO joinRoomByUser(User currentUser, String meetingCode) {
        // ...
        MeetingParticipant participant = participantService.handleAuthenticatedJoin(...);
        // DB: ghi WAITING nhưng CHƯA COMMIT
    
        notificationService.sendJoinRequest(...);  // ← (A) notification gửi ngay
        return response;
    }  // ← (B) transaction mới COMMIT ở đây
    ```
    
    **Timeline nguy hiểm:**
    
    ```
    t0: sendJoinRequest() gửi JOIN_REQUEST → host nhận
    t1: host frontend gọi GET /api/meetings/{code}/waiting  (hoặc dùng notification data trực tiếp)
    t2: DB query → participant vẫn là REJECT (chưa commit)
    t3: waiting list trả về rỗng
    t4: transaction commit → DB = WAITING (quá muộn)
    t5: host không thấy User A trong UI
    ```
    
    Spring SimpleBroker là **fire-and-forget**, không có cơ chế retry. Message gửi đi dù DB chưa sẵn sàng.
    
    **Tại sao lần 1 thỉnh thoảng vẫn đúng?** Lần join đầu tiên tạo mới participant (INSERT) — thường nhanh hơn UPDATE và ít xung đột hơn. Lần re-request là UPDATE từ REJECT → WAITING, dễ gặp window time hơn.
    
    ---
    
    ### Bug #2 — Race condition giữa `reject()` và `re-join()` (Lost Update)
    
    **Files:** `MeetingOrchestratorService.java` — `reject()` (line 188–200) và `joinRoomByUser()` (line 111–146)
    
    **Kịch bản:**
    
    ```
    [T-reject]   @Transactional reject() bắt đầu
    [T-reject]   UPDATE participant SET status = REJECT  ← chưa commit
    [T-reject]   sendReject() → User A nhận REJECTED ngay lập tức
                                                         User A ngay lập tức POST /join
    [T-rejoin]                                           @Transactional joinRoomByUser() bắt đầu
    [T-rejoin]                                           SELECT participant → status = WAITING (T-reject chưa commit!)
    [T-rejoin]                                           handleParticipantStatus(WAITING):
                                                           return existing  ← KHÔNG save (line 115)
    [T-rejoin]                                           sendJoinRequest() gửi JOIN_REQUEST
    [T-reject]   COMMIT → DB: status = REJECT
    [T-rejoin]                                           COMMIT → nothing to commit
    ```
    
    **Kết quả:** JOIN_REQUEST đã bay đến host, nhưng DB vẫn là `REJECT`. Host nhận notification, query waiting list → User A không có (status REJECT) → UI bỏ qua hoặc hiển thị sai.
    
    **File liên quan:** `ParticipantEntityService.java` — `handleParticipantStatus()` (line 86–116)
    
    ```java
    if (status == ParticipantStatus.REJECT || status == ParticipantStatus.LEFT) {
        existing.setParticipantStatus(ParticipantStatus.WAITING);
        return meetingParticipantRepository.save(existing);  // ← đúng path này OK
    }
    
    return existing;  // ← WAITING case: không save, không update createdAt
                      //   nếu rơi vào đây do race condition → không có gì commit
    ```
    
    ---
    
    ### Bug #3 — `WAITING_CANCELLED` gửi đến sai topic
    
    **File:** `NotificationService.java` — `sendWaitingCancelled()` (line 34–43)
    
    ```java
    public void sendWaitingCancelled(String meetingCode, Long participantId, String participantName) {
        MeetingWsMessage msg = buildMessage("WAITING_CANCELLED", meetingCode, participantId, participantName);
        messagingTemplate.convertAndSend(topic(meetingCode), msg);  // ← general topic (tất cả nhận)
        // Đúng phải là: waitingTopic(meetingCode)  (chỉ host cần biết)
    }
    ```
    
    **Hệ quả:** Tất cả participant trong phòng họp đều nhận event `WAITING_CANCELLED` — lộ thông tin waiting room không cần thiết.
    
    ---
    
    ### Bug #4 — `assertSameMeeting()` thiếu trong `reject()`
    
    **File:** `MeetingWebSocketController.java` — `reject()` (line 31–38)
    
    ```java
    @MessageMapping("/meeting/reject")
    public void reject(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {
        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);
        // ← THIẾU assertSameMeeting(headerAccessor, msg.getMeetingCode())
        this.meetingOrchestratorService.reject(
                msg.getMeetingCode(),
                msg.getTargetParticipantId(),
                hostParticipantId);
    }
    ```
    
    So sánh với `cancelJoin()` có đầy đủ:
    ```java
    @MessageMapping("/meeting/cancel-join")
    public void cancelJoin(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {
        Long callerParticipantId = getParticipantIdFromSession(headerAccessor);
        assertSameMeeting(headerAccessor, msg.getMeetingCode());  // ← có check này
        // ...
    }
    ```
    
    Service layer có kiểm tra nhưng thêm một lớp validation ở controller vẫn tốt hơn.
    
    ---
    
    ## 3. Đề Xuất Giải Pháp
    
    ### Fix #1 (Bug #1 & #2) — Publish notification SAU khi transaction commit
    
    **Cách:** Dùng Spring `ApplicationEventPublisher` + `@TransactionalEventListener(phase = AFTER_COMMIT)`.
    
    **Tại sao cách này?** `@TransactionalEventListener` đảm bảo event listener chỉ chạy sau khi outer transaction commit thành công — giải quyết cả hai bug (timing và race condition) mà không thay đổi business logic.
    
    **Bước 1:** Tạo event class
    
    ```java
    // src/main/java/com/meet/BE/domain/event/JoinRequestEvent.java
    public record JoinRequestEvent(String meetingCode, Long participantId, String participantName) {}
    ```
    
    **Bước 2:** Tạo listener
    
    ```java
    // src/main/java/com/meet/BE/listener/MeetingNotificationListener.java
    @Component
    @RequiredArgsConstructor
    public class MeetingNotificationListener {
        private final NotificationService notificationService;
    
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onJoinRequest(JoinRequestEvent event) {
            notificationService.sendJoinRequest(
                event.meetingCode(),
                event.participantId(),
                event.participantName()
            );
        }
    }
    ```
    
    **Bước 3:** Thay `sendJoinRequest()` trực tiếp bằng publish event trong `MeetingOrchestratorService`
    
    ```java
    // Trước:
    notificationService.sendJoinRequest(meetingCode, participant.getParticipantId(), currentUser.getFullName());
    
    // Sau:
    eventPublisher.publishEvent(
        new JoinRequestEvent(meetingCode, participant.getParticipantId(), currentUser.getFullName())
    );
    ```
    
    ---
    
    ### Fix #2 (Bug #2) — Pessimistic lock khi đọc participant trong luồng join
    
    **File:** `MeetingParticipantRepository.java`
    
    Thêm query với `PESSIMISTIC_WRITE` lock khi đọc participant để merge/rejoin:
    
    ```java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM MeetingParticipant p WHERE p.meeting.id = :meetingId AND p.user.id = :userId")
    Optional<MeetingParticipant> findByMeetingIdAndUserIdForUpdate(
        @Param("meetingId") Long meetingId,
        @Param("userId") Long userId
    );
    ```
    
    Dùng query này trong `ParticipantEntityService.findExistingUserParticipant()` khi context là join — ngăn `reject()` và `re-join()` đọc cùng row đồng thời.
    
    ---
    
    ### Fix #3 (Bug #3) — Sửa topic của `WAITING_CANCELLED`
    
    **File:** `NotificationService.java`
    
    ```java
    // Trước:
    messagingTemplate.convertAndSend(topic(meetingCode), msg);
    
    // Sau:
    messagingTemplate.convertAndSend(waitingTopic(meetingCode), msg);
    ```
    
    ---
    
    ### Fix #4 (Bug #4) — Thêm `assertSameMeeting()` vào `reject()` và `admit()`
    
    **File:** `MeetingWebSocketController.java`
    
    ```java
    @MessageMapping("/meeting/reject")
    public void reject(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {
        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);
        assertSameMeeting(headerAccessor, msg.getMeetingCode());  // ← thêm vào
        this.meetingOrchestratorService.reject(...);
    }
    
    @MessageMapping("/meeting/accept")
    public void admit(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {
        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);
        assertSameMeeting(headerAccessor, msg.getMeetingCode());  // ← thêm vào
        this.meetingOrchestratorService.admit(...);
    }
    ```
    
    ---
    
    ## 4. Thứ Tự Ưu Tiên
    
    | # | Fix | Mức độ | Ảnh hưởng |
    |---|---|---|---|
    | 1 | `@TransactionalEventListener` cho `sendJoinRequest()` | Critical | Giải quyết bug chính được báo cáo |
    | 2 | Pessimistic lock trên participant read | High | Ngăn race condition |
    | 3 | Sửa topic `WAITING_CANCELLED` | Medium | Security/correctness |
    | 4 | `assertSameMeeting()` trong reject/admit | Low | Defense in depth |
    
    ---
    
    ## 5. Files Cần Thay Đổi
    
    ```
    src/main/java/com/meet/BE/
    ├── domain/event/
    │   └── JoinRequestEvent.java              ← TẠO MỚI
    ├── listener/
    │   └── MeetingNotificationListener.java   ← TẠO MỚI
    ├── service/user/
    │   ├── MeetingOrchestratorService.java    ← SỬA: dùng eventPublisher thay vì gọi trực tiếp
    │   └── NotificationService.java          ← SỬA: fix topic WAITING_CANCELLED
    ├── controller/
    │   └── MeetingWebSocketController.java   ← SỬA: thêm assertSameMeeting
    └── repository/
        └── MeetingParticipantRepository.java ← SỬA: thêm query với PESSIMISTIC_WRITE
    ```
