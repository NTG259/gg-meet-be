package com.meet.BE.service.refactor;

import com.meet.BE.domain.message.MeetingWsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

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
    private String participantTopic(String meetingCode, Long participantId) {
        return topic(meetingCode) + "/participant/" + participantId;
    }

    public void sendJoinRequest(String meetingCode, Long participantId, String participantName) {
        MeetingWsMessage msg = buildMessage(
                "JOIN_REQUEST",
                meetingCode,
                participantId,
                participantName
        );

        messagingTemplate.convertAndSend(waitingTopic(meetingCode), msg);
    }

    public void sendWaitingCancelled(String meetingCode, Long participantId, String participantName) {
        MeetingWsMessage msg = buildMessage(
                "WAITING_CANCELLED",
                meetingCode,
                participantId,
                participantName
        );

        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    public void sendAdmit(String meetingCode, Long targetParticipantId, String participantName) {
        MeetingWsMessage msg = buildMessage(
                "ADMITTED",
                meetingCode,
                targetParticipantId,
                participantName
        );

        messagingTemplate.convertAndSend(participantTopic(meetingCode, targetParticipantId), msg);
        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    public void sendUserKicked(String meetingCode, Long targetParticipantId, String participantName) {
        MeetingWsMessage msg = buildMessage(
                "USER_KICKED",
                meetingCode,
                targetParticipantId,
                participantName
        );

        messagingTemplate.convertAndSend(participantTopic(meetingCode, targetParticipantId), msg);
        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    public void sendReject(String meetingCode, Long participantId, Long hostId, String name) {

        messagingTemplate.convertAndSend(
                participantTopic(meetingCode, participantId),
                buildMessage("REJECTED", meetingCode, participantId, name)
        );

        messagingTemplate.convertAndSend(
                participantTopic(meetingCode, hostId),
                buildMessage("REJECT_SUCCESS", meetingCode, participantId, name)
        );
    }

    public void sendMeetingEnded(String meetingCode) {
        MeetingWsMessage msg = new MeetingWsMessage();
        msg.setMeetingCode(meetingCode);
        msg.setAction("MEETING_ENDED");

        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    public void sendParticipantLeft(String meetingCode, Long participantId) {
        MeetingWsMessage msg = buildMessage(
                "PARTICIPANT_LEFT",
                meetingCode,
                participantId,
                null
        );

        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    public void sendScreenShareRequested(String meetingCode, Long requesterId, String requesterName) {
        MeetingWsMessage msg = buildMessage("SCREEN_SHARE_REQUESTED", meetingCode, requesterId, requesterName);
        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    public void sendScreenShareApproved(String meetingCode, Long requesterId) {
        MeetingWsMessage msg = buildMessage("SCREEN_SHARE_APPROVED", meetingCode, requesterId, null);
        messagingTemplate.convertAndSend(participantTopic(meetingCode, requesterId), msg);
    }

    public void sendScreenShareRejected(String meetingCode, Long requesterId) {
        MeetingWsMessage msg = buildMessage("SCREEN_SHARE_REJECTED", meetingCode, requesterId, null);
        messagingTemplate.convertAndSend(participantTopic(meetingCode, requesterId), msg);
    }

    public void sendScreenShareStopped(String meetingCode, Long targetId) {
        MeetingWsMessage msg = buildMessage("SCREEN_SHARE_STOPPED", meetingCode, targetId, null);
        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    public void sendRoomSettingsChanged(String meetingCode, boolean allowParticipantShareScreen) {
        MeetingWsMessage msg = new MeetingWsMessage();
        msg.setMeetingCode(meetingCode);
        msg.setAction("ROOM_SETTINGS_CHANGED");
        msg.setExtra(Map.of("allowParticipantShareScreen", allowParticipantShareScreen));
        messagingTemplate.convertAndSend(topic(meetingCode), msg);
    }

    public void sendMuteSettingsChanged(String meetingCode, boolean allowParticipantUnmute) {
        MeetingWsMessage msg = new MeetingWsMessage();
        msg.setMeetingCode(meetingCode);
        msg.setAction("ROOM_SETTINGS_CHANGED");
        msg.setExtra(Map.of("allowParticipantUnmute", allowParticipantUnmute));
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
