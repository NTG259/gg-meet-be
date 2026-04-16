package com.meet.BE.controller.socket;

import io.livekit.server.WebhookReceiver;
import livekit.LivekitWebhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/livekit")
@RequiredArgsConstructor
public class LiveKitWebhookController {

    private final WebhookReceiver receiver =
            new WebhookReceiver("devKey", "8f9d2c1a7b6e5f4d3c2b1a0e9f8d7c6b");

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String body,
            @RequestHeader("Authorization") String authHeader) {

        System.out.println(">>> recv Webhook");
        try {
            LivekitWebhook.WebhookEvent event =
                    receiver.receive(body, authHeader);

            switch (event.getEvent()) {
                case "room_started":
                    String roomName = event.getRoom().getName();
                    System.out.println("Room ended: " + roomName);
                    break;

                case "participant_joined":
                    break;

                case "participant_left":
                    break;
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}
