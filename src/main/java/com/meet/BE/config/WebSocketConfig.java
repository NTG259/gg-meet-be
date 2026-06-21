package com.meet.BE.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.meet.BE.domain.dto.MeetingTokenPrincipal;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.service.user.SecurityService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final SecurityService securityService;

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/api");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/server")
                .setAllowedOriginPatterns(resolveAllowedOrigins())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticateWebSocketSession(accessor);
                }

                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    authorizeSubscription(accessor);
                }
                return message;
            }
        });
    }

    private void authenticateWebSocketSession(StompHeaderAccessor accessor) {
        String meetingToken = accessor.getFirstNativeHeader("Meeting-Token");

        if (meetingToken == null || meetingToken.isBlank()) {
            throw new IllegalArgumentException("Unauthorized");
        }

        try {
            MeetingTokenPrincipal principal = securityService.parseMeetingToken(meetingToken);
            Map<String, Object> sessionAttributes = getOrCreateSessionAttributes(accessor);

            sessionAttributes.put("participantId", principal.participantId());
            sessionAttributes.put("meetingCode", principal.meetingCode());
            sessionAttributes.put("role", principal.role());
            log.info(
                    "WebSocket authenticated participantId={}, meetingCode={}, role={}",
                    principal.participantId(),
                    principal.meetingCode(),
                    principal.role());
        } catch (Exception e) {
            log.error("WebSocket token invalid: {}", e.getMessage());
            throw new IllegalArgumentException("Unauthorized");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new IllegalArgumentException("Unauthorized");
        }

        String destination = accessor.getDestination();
        String meetingCode = readStringSessionAttribute(sessionAttributes, "meetingCode");
        String role = readStringSessionAttribute(sessionAttributes, "role");
        Long participantId = readLongSessionAttribute(sessionAttributes, "participantId");

        if (!isDestinationAllowed(destination, meetingCode, role, participantId)) {
            log.warn(
                    "Rejected unauthorized STOMP subscription: destination={}, participantId={}, meetingCode={}, role={}",
                    destination,
                    participantId,
                    meetingCode,
                    role);
            throw new IllegalArgumentException("Unauthorized subscription");
        }
    }

    private boolean isDestinationAllowed(String destination, String meetingCode, String role, Long participantId) {
        if (destination == null || meetingCode == null || participantId == null) {
            return false;
        }

        String prefix = "/topic/meeting/";
        if (!destination.startsWith(prefix)) {
            return false;
        }

        String[] segments = destination.substring(prefix.length()).split("/");
        if (segments.length == 0 || !meetingCode.equals(segments[0])) {
            return false;
        }

        if (segments.length == 1) {
            return true;
        }

        if (segments.length == 2 && "waiting".equals(segments[1])) {
            return Role.HOST.name().equals(role);
        }

        return segments.length == 3
                && "participant".equals(segments[1])
                && participantId.toString().equals(segments[2]);
    }

    private Map<String, Object> getOrCreateSessionAttributes(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            sessionAttributes = new HashMap<>();
            accessor.setSessionAttributes(sessionAttributes);
        }
        return sessionAttributes;
    }

    private String readStringSessionAttribute(Map<String, Object> sessionAttributes, String key) {
        Object value = sessionAttributes.get(key);
        return value == null ? null : value.toString();
    }

    private Long readLongSessionAttribute(Map<String, Object> sessionAttributes, String key) {
        Object value = sessionAttributes.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private String[] resolveAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }
}
