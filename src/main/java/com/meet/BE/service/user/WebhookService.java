package com.meet.BE.service.user;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WebhookService {
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final NotificationService notificationService;
    private final LiveKitService liveKitService;

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

    public void removeUnauthorizedParticipant(String meetingCode, String identity) {
        if (identity == null || identity.isBlank()) {
            return;
        }

        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode).orElse(null);
        if (meeting == null) {
            liveKitService.removeParticipantIfPresent(meetingCode, identity);
            return;
        }

        Optional<MeetingParticipant> participant = findParticipantByIdentity(meeting, identity);
        if (participant.isEmpty() || participant.get().getParticipantStatus() != ParticipantStatus.ACCEPT) {
            liveKitService.removeParticipantIfPresent(meetingCode, identity);
        }
    }

    private Optional<MeetingParticipant> findParticipantByIdentity(Meeting meeting, String identity) {
        try {
            Long userId = Long.parseLong(identity);
            Optional<MeetingParticipant> participant =
                    meetingParticipantRepository.findByMeetingIdAndUserId(meeting.getId(), userId);
            if (participant.isPresent()) {
                return participant;
            }
        } catch (NumberFormatException ignored) {
            // Guest identities are not numeric in the normal FE flow.
        }

        return meetingParticipantRepository.findByMeetingIdAndGuestId(meeting.getId(), identity);
    }
}
