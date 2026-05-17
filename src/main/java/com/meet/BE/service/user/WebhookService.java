package com.meet.BE.service.user;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.message.MeetingWsMessage;
import com.meet.BE.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WebhookService {
    private final MeetingRepository meetingRepository;
    private final NotificationService notificationService;

    /**
     * Hàm này được gọi tự động bởi Webhook của LiveKit khi phòng bị đóng
     * (Do timeout không có ai trong phòng, hoặc phòng bị giải tán)
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoEndMeeting(String meetingCode) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElse(null);

        if (meeting == null) {
            return;
        }

        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
            return;
        }

        meeting.setStatus(MeetingStatus.ENDED);
        meeting.setEndTime(LocalDateTime.now());
        meetingRepository.save(meeting);

        notificationService.sendMeetingEnded(meetingCode);
    }
}
