package com.meet.BE.controller.socket;

import com.meet.BE.service.refactor.WebhookService;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitWebhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/livekit")
@RequiredArgsConstructor
@Slf4j
public class LiveKitWebhookController {

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    // Inject service vào để gọi hàm autoEndMeeting
    private final WebhookService webhookService;

    @Bean
    public WebhookReceiver webhookReceiver() {
        return new WebhookReceiver(apiKey, apiSecret);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String body,
            @RequestHeader("Authorization") String authHeader) {

        try {
            LivekitWebhook.WebhookEvent event = webhookReceiver().receive(body, authHeader);
            String eventName = event.getEvent();

            // Log nhẹ nhàng để biết Webhook đang hoạt động
            log.info(">>> [LIVEKIT WEBHOOK] Received Event: {}", eventName);

            // BẮT SỰ KIỆN PHÒNG KẾT THÚC
            if ("room_finished".equals(eventName)) {
                // Trong LiveKit, Tên phòng (Room Name) chính là Meeting Code của chúng ta
                String meetingCode = event.getRoom().getName();
                log.info(">>> [LIVEKIT WEBHOOK] Phòng {} đã đóng. Đang cập nhật DB...", meetingCode);
                webhookService.autoEndMeeting(meetingCode);
            }
            // Trả về ACK cho Livekit, nếu không nó sẽ spam gửi lại
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error(">>> [LIVEKIT WEBHOOK ERROR]: Xác thực thất bại hoặc có lỗi - {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
}