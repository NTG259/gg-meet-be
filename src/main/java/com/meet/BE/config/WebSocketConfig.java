package com.meet.BE.config;

import com.meet.BE.service.refactor.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // Dùng để decode meeting token ngay tại bước CONNECT của WebSocket.
    private final SecurityService securityService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        /*
            server -> client
            subscribe mẫu :
               - /topic/meeting/{code}/waiting
               - /topic/meeting/{code}/chat
             Note : topic để broadcast, queue để 1-1
        */
        registry.setApplicationDestinationPrefixes("/api");
        /*
            client -> server
            action mẫu :
                - /api/meeting/join
                - /api/meeting/admit
         */
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/server")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        // Connect : ws://localhost:8080/server
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Interceptor này chạy cho mọi frame client -> server.
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                // 1. Kiểm tra nếu là lệnh CONNECT (Lúc Frontend vừa gọi stompClient.connect)
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                    // 2. Lấy Token từ Header "Meeting-Token" mà FE gửi lên
                    String meetingToken = accessor.getFirstNativeHeader("Meeting-Token");

                    if (meetingToken == null || meetingToken.isBlank()) {
                        // Không cho phép vào WS nếu thiếu Meeting-Token.
                        throw new IllegalArgumentException("Unauthorized");
                    }

                    try {
                        // 3. Giải mã Token lấy ParticipantId
                        Long participantId = securityService.getParticipantIdFromToken(meetingToken);

                        // 4. Lưu participantId vào session để tái sử dụng ở các @MessageMapping
                        // => FE không cần gửi participantId trong từng action admit/reject.
                        accessor.getSessionAttributes().put("participantId", participantId);
                        log.info("WebSocket authenticated participantId={}", participantId);
                    } catch (Exception e) {
                        log.error("WebSocket token invalid: {}", e.getMessage());
                        throw new IllegalArgumentException("Unauthorized"); // Chặn không cho vào
                    }
                }
                return message;
            }
        });
    }
}
