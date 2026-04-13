package com.meet.BE.controller.socket;

import com.meet.BE.domain.message.MeetingMessage;
import com.meet.BE.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MeetingWsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ParticipantService participantService;

    // USER JOIN WAITING ROOM
    @MessageMapping("/join")
    public void join(MeetingMessage msg) {

        participantService.joinWaiting(msg);

        // báo cho host
        messagingTemplate.convertAndSend(
                "/topic/meeting/" + msg.getMeetingCode() + "/waiting",
                msg
        );
    }

    // HOST ADMIT USER
    @MessageMapping("/admit")
    public void admit(MeetingMessage msg) {

        participantService.admit(msg.getMeetingCode(), msg.getUserId());

        // gửi riêng cho user
        messagingTemplate.convertAndSendToUser(
                String.valueOf(msg.getUserId()),
                "/queue/admitted",
                msg
        );

        // broadcast vào room
        messagingTemplate.convertAndSend(
                "/topic/meeting/" + msg.getMeetingCode(),
                msg
        );
    }

    // HOST REJECT USER
    @MessageMapping("/reject")
    public void reject(MeetingMessage msg) {

        participantService.reject(msg.getMeetingCode(), msg.getUserId());

        messagingTemplate.convertAndSendToUser(
                String.valueOf(msg.getUserId()),
                "/queue/rejected",
                msg
        );
    }
}
