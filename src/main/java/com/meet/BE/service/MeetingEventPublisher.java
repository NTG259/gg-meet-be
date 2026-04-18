package com.meet.BE.service;

import com.meet.BE.domain.message.MeetingWsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Bắn sự kiện có người xin vào phòng (Waiting Room) cho Host
     */
    public void publishJoinRequestEvent(String meetingCode, Long participantId, String displayName) {
        log.info("Publishing JOIN_REQUEST for participant {} in meeting {}", displayName, meetingCode);

        MeetingWsMessage socketMessage = new MeetingWsMessage();
        socketMessage.setMeetingCode(meetingCode);
        socketMessage.setTargetParticipantId(participantId);
        socketMessage.setTargetName(displayName);
        socketMessage.setAction("JOIN_REQUEST");

        String destination = "/topic/meeting/" + meetingCode + "/waiting";
        messagingTemplate.convertAndSend(destination, socketMessage);
    }

}