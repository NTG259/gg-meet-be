package com.meet.BE.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Long participantId = headerAccessor.getSessionAttributes() != null
                ? (Long) headerAccessor.getSessionAttributes().get("participantId")
                : null;

        if (participantId != null) {
            log.info("Participant disconnected. SessionID: {}, ParticipantID: {}", sessionId, participantId);
        } else {
            log.warn("Unknown WebSocket connection dropped. SessionID: {}", sessionId);
        }
    }
}
