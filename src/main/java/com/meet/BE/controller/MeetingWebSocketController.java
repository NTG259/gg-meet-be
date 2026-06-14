package com.meet.BE.controller;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.message.MeetingWsMessage;
import com.meet.BE.service.user.MeetingOrchestratorService;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MeetingWebSocketController {

    private final MeetingOrchestratorService meetingOrchestratorService;

    @MessageMapping("/meeting/accept")
    public void admit(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {
        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);
        assertSameMeeting(headerAccessor, msg.getMeetingCode());
        MeetingParticipant participant = this.meetingOrchestratorService.admit(msg.getMeetingCode(), msg.getTargetParticipantId(), hostParticipantId);
    }

    @MessageMapping("/meeting/reject")
    public void reject(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {
        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);
        assertSameMeeting(headerAccessor, msg.getMeetingCode());
        this.meetingOrchestratorService.reject(
                msg.getMeetingCode(),
                msg.getTargetParticipantId(),
                hostParticipantId);
    }

    @MessageMapping("/meeting/cancel-join")
    public void cancelJoin(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {
        Long callerParticipantId = getParticipantIdFromSession(headerAccessor);
        assertSameMeeting(headerAccessor, msg.getMeetingCode());

        if (!callerParticipantId.equals(msg.getTargetParticipantId())) {
            throw new BusinessException(ErrorCode.NOT_PERMIT);
        }

        meetingOrchestratorService.cancelJoin(callerParticipantId, msg.getMeetingCode());
    }


    private Long getParticipantIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object participantId = headerAccessor.getSessionAttributes().get("participantId");
        if (!(participantId instanceof Long)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return (Long) participantId;
    }

    private void assertSameMeeting(SimpMessageHeaderAccessor headerAccessor, String meetingCode) {
        if (headerAccessor.getSessionAttributes() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object sessionMeetingCode = headerAccessor.getSessionAttributes().get("meetingCode");
        if (!meetingCode.equals(sessionMeetingCode)) {
            throw new BusinessException(ErrorCode.NOT_PERMIT);
        }
    }

    @MessageMapping("/meeting/kickout")
    public void kickout(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {
        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);
        Boolean isBan = msg.getIsBan() != null ? msg.getIsBan() : true;
        this.meetingOrchestratorService.kickout(
                msg.getMeetingCode(),
                msg.getTargetParticipantId(),
                hostParticipantId,
                isBan);
    }
}
