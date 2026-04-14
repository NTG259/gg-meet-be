package com.meet.BE.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = headerAccessor.getSessionId();

        // Lấy participantId đã được gán vào session lúc handshake/connect
        Long participantId = (Long) headerAccessor.getSessionAttributes().get("participantId");

        if (participantId != null) {
            log.info("Participant disconnected. SessionID: {}, ParticipantID: {}", sessionId, participantId);

            // TODO: Gọi ParticipantService để set trạng thái OFFLINE (hoặc leftAt = now())
            // participantService.handleParticipantLeave(participantId);

            // TODO: Broadcast cho những người trong phòng biết người này đã thoát
        } else {
            log.warn("Unknown WebSocket connection dropped. SessionID: {}", sessionId);
        }
    }
}