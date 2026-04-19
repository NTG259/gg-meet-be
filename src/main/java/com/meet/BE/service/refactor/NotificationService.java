package com.meet.BE.service.refactor;

import com.meet.BE.domain.message.MeetingWsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private String topic(String meetingCode) {
        return "/topic/meeting/" + meetingCode;
    }

    private String waitingTopic(String meetingCode) {
        return topic(meetingCode) + "/waiting";
    }

    public void sendJoinRequest(String meetingCode, Long participantId, String name) {
        MeetingWsMessage msg = buildMessage(
                "JOIN_REQUEST",
                meetingCode,
                participantId,
                name
        );

        messagingTemplate.convertAndSend(waitingTopic(meetingCode), msg);
    }

    public void sendWaitingCancelled(String meetingCode, Long participantId, String name) {
        MeetingWsMessage msg = buildMessage(
                "WAITING_CANCELLED",
                meetingCode,
                participantId,
                name
        );

        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    public void sendAdmit(String meetingCode, Long participantId, String name) {
        MeetingWsMessage msg = buildMessage(
                "ADMIT",
                meetingCode,
                participantId,
                name
        );

        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    public void sendReject(String meetingCode, Long participantId, String name) {
        MeetingWsMessage msg = buildMessage(
                "REJECT",
                meetingCode,
                participantId,
                name
        );

        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    private MeetingWsMessage buildMessage(String action, String meetingCode,
                                          Long participantId, String name) {
        MeetingWsMessage msg = new MeetingWsMessage();
        msg.setAction(action);
        msg.setMeetingCode(meetingCode);
        msg.setTargetParticipantId(participantId);
        msg.setTargetName(name);
        return msg;
    }
}
