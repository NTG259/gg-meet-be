package com.meet.BE.controller.socket;

import io.livekit.server.WebhookReceiver;
import livekit.LivekitWebhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/livekit")
@RequiredArgsConstructor
public class LiveKitWebhookController {

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    @Bean
    public WebhookReceiver webhookReceiver() {
        // Đảm bảo apiKey và apiSecret ở đây khớp 100% với server LiveKit
        return new WebhookReceiver(apiKey, apiSecret);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String body,
            @RequestHeader("Authorization") String authHeader) {

        try {
            LivekitWebhook.WebhookEvent event = webhookReceiver().receive(body, authHeader);

            // Sử dụng StringBuilder để gom các thông tin có ý nghĩa thành 1 dòng log
            StringBuilder logMsg = new StringBuilder();

            // 1. Luôn luôn có tên sự kiện
            logMsg.append(">>> [WEBHOOK] Event: ").append(event.getEvent());

            // 2. Lấy thông tin Room (nếu có)
            logMsg.append(" | Room: ").append(event.getRoom().getName())
                    .append(" (").append(event.getRoom().getSid()).append(")");

            // 3. Lấy thông tin Participant (nếu có)
            logMsg.append(" | Participant: ").append(event.getParticipant().getIdentity());

            // Lấy thêm tên hiển thị của participant nếu họ có cài đặt
            String pName = event.getParticipant().getName();
            if (!pName.isEmpty()) {
                logMsg.append(" [").append(pName).append("]");
            }

            // 4. Lấy thông tin Track (Audio/Video - nếu có)
            logMsg.append(" | Track SID: ").append(event.getTrack().getSid())
                    .append(" (").append(event.getTrack().getType()).append(")");

//            log.info
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println(">>> [WEBHOOK ERROR]: " + e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
}
