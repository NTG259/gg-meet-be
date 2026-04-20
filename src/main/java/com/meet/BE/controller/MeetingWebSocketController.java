package com.meet.BE.controller;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.message.MeetingWsMessage;
import com.meet.BE.service.ParticipantService;
import com.meet.BE.service.refactor.MeetingOrchestratorService;
import com.meet.BE.service.refactor.ParticipantServiceRF;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MeetingWebSocketController {

    private final ParticipantService participantService;
    private final MeetingOrchestratorService meetingOrchestratorService;
    private final ParticipantServiceRF participantServiceRF;

    @MessageMapping("/meeting/accept")
    public void admit(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {

        // Lấy ID của Host thao tác từ WebSocket Session (đã lưu lúc check Token)

        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);
        MeetingParticipant participant = this.meetingOrchestratorService.admit(msg.getMeetingCode(), msg.getTargetParticipantId(), hostParticipantId);
    }

    @MessageMapping("/meeting/reject")
    public void reject(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {
        Long hostParticipantId = getParticipantIdFromSession(headerAccessor);
        this.meetingOrchestratorService.reject(
                msg.getMeetingCode(),
                msg.getTargetParticipantId(),
                hostParticipantId);
    }

    @MessageMapping("/meeting/cancel-join")
    public void cancelJoin(SimpMessageHeaderAccessor headerAccessor, MeetingWsMessage msg) {
        participantServiceRF.cancelWaiting(msg.getTargetParticipantId(), msg.getMeetingCode());
    }


    private Long getParticipantIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() == null) {
            return null;
        }
        return (Long) headerAccessor.getSessionAttributes().get("participantId");
    }
}