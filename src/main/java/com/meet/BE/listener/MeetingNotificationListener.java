package com.meet.BE.listener;

import com.meet.BE.domain.event.JoinRequestEvent;
import com.meet.BE.service.user.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MeetingNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJoinRequest(JoinRequestEvent event) {
        notificationService.sendJoinRequest(
                event.meetingCode(),
                event.participantId(),
                event.participantName()
        );
    }
}
