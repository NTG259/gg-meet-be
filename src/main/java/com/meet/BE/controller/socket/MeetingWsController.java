package com.meet.BE.controller.socket;

import com.meet.BE.domain.message.MeetingWsMessage;
import com.meet.BE.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MeetingWsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ParticipantService participantService;

    // HOST ADMIT USER
    @MessageMapping("/meeting/accept")
    public void admit(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {

        // Lấy ID của Host thao tác từ WebSocket Session (đã lưu lúc check Token)
        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);

        log.info("WS ADMIT request: hostParticipantId={}, targetParticipantId={}, meetingCode={}",
                hostParticipantId, msg.getTargetParticipantId(), msg.getMeetingCode());

        try {
            // Duyệt ở backend bằng actorParticipantId lấy từ session WS (không tin payload FE).
            // Service giờ chỉ cần ID của bản ghi Participant để duyệt
            participantService.admit(hostParticipantId, msg.getTargetParticipantId(), msg.getMeetingCode());

            msg.setAction("ADMITTED");

            // 1. Gửi tin nhắn RIÊNG cho người được duyệt (Dùng topic có chứa ID của họ)
            // Frontend của Khách sẽ subscribe: /topic/meeting/{meetingCode}/participant/{participantId}
            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + msg.getMeetingCode() + "/participant/" + msg.getTargetParticipantId(),
                    msg
            );
            log.info("Sent admit message to participant topic: {}", msg.getTargetParticipantId());

            // 2. Broadcast vào chung cả room (Tùy chọn: Để báo cho mọi người biết có người mới vào)
            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + msg.getMeetingCode(),
                    msg
            );

        } catch (RuntimeException e) {
            log.error("ADMIT failed: targetParticipantId={}, error={}",
                    msg.getTargetParticipantId(), e.getMessage());
            throw e; // Tùy chiến lược xử lý lỗi mà bạn có thể bọc lại
        }
    }

    // HOST REJECT USER
    @MessageMapping("/meeting/reject")
    public void reject(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {

        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);

        log.info("WS REJECT request: hostParticipantId={}, targetParticipantId={}, meetingCode={}",
                hostParticipantId, msg.getTargetParticipantId(), msg.getMeetingCode());

        try {
            // Từ chối cũng kiểm tra cùng cơ chế quyền HOST như luồng duyệt.
            participantService.reject(hostParticipantId, msg.getTargetParticipantId(), msg.getMeetingCode());

            // 1. Gửi tin nhắn RIÊNG cho người bị từ chối
            MeetingWsMessage messageParticipant = new MeetingWsMessage();
            messageParticipant.setAction("REJECTED");
            messageParticipant.setMeetingCode(msg.getMeetingCode());
            messageParticipant.setTargetParticipantId(msg.getTargetParticipantId());
            messageParticipant.setTargetName(msg.getTargetName());
            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + messageParticipant.getMeetingCode()
                            + "/participant/" + messageParticipant.getTargetParticipantId(),
                    messageParticipant
            );
            log.info("Sent reject message to participant topic: {}", msg.getTargetParticipantId());

            // 2. Gửi tin nhắn báo thành công về lại cho HOST
            // Đổi action để Frontend của Host phân biệt được sự kiện này và trigger fetch lại danh sách chờ
            MeetingWsMessage messageHost = new MeetingWsMessage();
            messageHost.setAction("REJECT_SUCCESS");
            messageHost.setMeetingCode(msg.getMeetingCode());
            messageHost.setTargetParticipantId(msg.getTargetParticipantId());
            messageHost.setTargetName(msg.getTargetName());
            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + messageHost.getMeetingCode()
                            + "/participant/" + hostParticipantId,
                    messageHost
            );
            log.info("Sent reject success update to host topic: {}", hostParticipantId);

        } catch (RuntimeException e) {
            log.error("REJECT failed: targetParticipantId={}, error={}",
                    msg.getTargetParticipantId(), e.getMessage());
            throw e;
        }
    }

    @MessageMapping("/meeting/cancel-join")
    public void cancelJoin(SimpMessageHeaderAccessor headerAccessor,
                           MeetingWsMessage msg) {
        // Lấy ID của Host thao tác từ WebSocket Session (đã lưu lúc check Token)
        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);
        try {
            // Duyệt ở backend bằng actorParticipantId lấy từ session WS (không tin payload FE).
            // Service giờ chỉ cần ID của bản ghi Participant để duyệt
            participantService.cancel(msg.getTargetParticipantId(), msg.getMeetingCode());

            MeetingWsMessage messageHost = new MeetingWsMessage();
            messageHost.setAction("CANCEL_SUCCESS");
            messageHost.setMeetingCode(msg.getMeetingCode());
            messageHost.setTargetParticipantId(msg.getTargetParticipantId());
            messageHost.setTargetName(msg.getTargetName());
            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + messageHost.getMeetingCode()
                            + "/participant/" + hostParticipantId,
                    messageHost
            );
            log.info("Sent cancel join to host topic: {}", hostParticipantId);

        } catch (RuntimeException e) {
            log.error("CANCEL failed: targetParticipantId={}, error={}",
                    msg.getTargetParticipantId(), e.getMessage());
            throw e;
        }
    }

    private Long getParticipantIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() == null) {
            return null;
        }
        return (Long) headerAccessor.getSessionAttributes().get("participantId");
    }
}