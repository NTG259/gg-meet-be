package com.meet.BE.service.refactor;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.request.GuestJoinRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParticipantServiceRF {
    private final ParticipantEntityService participantEntityService;
    private final MeetingEntityService meetingEntityService;
    private final NotificationService notificationService;


    @Transactional
    public MeetingParticipant handleGuestJoin(Meeting meeting, GuestJoinRequest guestRequest) {
        // Cho chức năng Guest
        Optional<MeetingParticipant> existingOpt = this.participantEntityService
                .findExistingGuestParticipant(meeting, guestRequest);

        // Kiểm tra người này đã vào phòng lần nào chưa
        if (existingOpt.isPresent()) {
            return this.participantEntityService.handleParticipantStatus(existingOpt.get());
        } else {
            return this.participantEntityService.createGuestParticipant(meeting, guestRequest);
        }
    }

    public MeetingParticipant handleUserJoin(Meeting meeting, User user) {
        Optional<MeetingParticipant> existingOpt =
                participantEntityService.findExistingUserParticipant(meeting, user);
        // Nếu đã từng vào
        if (existingOpt.isPresent()) {
            return participantEntityService.handleParticipantStatus(existingOpt.get());
        }
        // Nếu user chưa từng vào thì phải waiting
        return participantEntityService.createUserParticipant(meeting, user, ParticipantStatus.WAITING);
    }
}
