package com.meet.BE.controller.socket;

import com.meet.BE.controller.auth.AuthController;
import com.meet.BE.domain.message.MeetingMessage;
import com.meet.BE.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MeetingWsController {
    private static final Logger log = LoggerFactory.getLogger(MeetingWsController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ParticipantService participantService;

    // USER JOIN WAITING ROOM
    @MessageMapping("/meet.join")
    public void join(MeetingMessage msg) {

        log.info("WS JOIN request: meetingCode={}, userId={}, email={}",
                msg.getMeetingCode(), msg.getUserId(), msg.getEmail());

        try {
            participantService.joinWaiting(msg);

            log.info("User joined waiting room: meetingCode={}, userId={}, email={}",
                    msg.getMeetingCode(), msg.getUserId(), msg.getEmail());

            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + msg.getMeetingCode() + "/waiting",
                    msg
            );

            log.info("Broadcast waiting list to topic: /topic/meeting/{}/waiting",
                    msg.getMeetingCode());

        } catch (Exception e) {
            log.error("JOIN failed: meetingCode={}, userId={}, error={}",
                    msg.getMeetingCode(), msg.getUserId(), e.getMessage());
            throw e;
        }
    }

    // HOST ADMIT USER
    @MessageMapping("/meeting.accept")
    public void admit(MeetingMessage msg) {

        log.info("WS ADMIT request: meetingCode={}, userId={}",
                msg.getMeetingCode(), msg.getUserId());

        try {
            participantService.admit(msg.getMeetingCode(), msg.getUserId());

            log.info("User admitted: meetingCode={}, userId={}",
                    msg.getMeetingCode(), msg.getUserId());

            // gửi riêng user
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(msg.getUserId()),
                    "/queue/admitted",
                    msg
            );

            log.info("Sent admit message to user queue: userId={}", msg.getUserId());

            // broadcast vào room
            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + msg.getMeetingCode(),
                    msg
            );

            log.info("Broadcast user joined meeting: meetingCode={}", msg.getMeetingCode());

        } catch (Exception e) {
            log.error("ADMIT failed: meetingCode={}, userId={}, error={}",
                    msg.getMeetingCode(), msg.getUserId(), e.getMessage());
            throw e;
        }
    }

    // HOST REJECT USER
    @MessageMapping("/meeting.reject")
    public void reject(MeetingMessage msg) {

        log.info("WS REJECT request: meetingCode={}, userId={}",
                msg.getMeetingCode(), msg.getUserId());

        try {
            participantService.reject(msg.getMeetingCode(), msg.getUserId());

            log.info("User rejected: meetingCode={}, userId={}",
                    msg.getMeetingCode(), msg.getUserId());

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(msg.getUserId()),
                    "/queue/rejected",
                    msg
            );

            log.info("Sent reject message to user queue: userId={}", msg.getUserId());

        } catch (Exception e) {
            log.error("REJECT failed: meetingCode={}, userId={}, error={}",
                    msg.getMeetingCode(), msg.getUserId(), e.getMessage());
            throw e;
        }
    }
}
