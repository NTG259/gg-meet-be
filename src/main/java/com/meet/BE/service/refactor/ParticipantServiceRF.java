package com.meet.BE.service.refactor;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParticipantServiceRF {
    private final ParticipantEntityService participantEntityService;

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

    public MeetingParticipant handleInvitedJoin(Meeting meeting, String email, User user, GuestJoinRequest guest) {
        Optional<MeetingParticipant> invited = this.participantEntityService.findInvitedUser(meeting, email);

        if (invited.isPresent()) {
            MeetingParticipant participant = invited.get();
            if (user != null) {
                participant.setUser(user);
            } else {
                participant.setGuestId(guest.getGuestId());
                participant.setGuestName(guest.getGuestName());
            }
            return this.participantEntityService.handleParticipantStatus(participant);
        } else {
            if (user != null) {
                return handleUserJoin(meeting, user);
            } else {
                return handleGuestJoin(meeting, guest);
            }
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

    @Transactional
    public MeetingParticipant handleAcceptParticipant(MeetingParticipant participant) {
        if (participant.getParticipantStatus() != ParticipantStatus.WAITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        return participantEntityService.updateParticipantStatus(participant, ParticipantStatus.ACCEPT);
    }

    @Transactional
    public MeetingParticipant handleRejectParticipant(MeetingParticipant participant) {
        if (participant.getParticipantStatus() != ParticipantStatus.WAITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        return participantEntityService.updateParticipantStatus(participant, ParticipantStatus.REJECT);
    }

    @Transactional
    public MeetingParticipant cancelWaiting(Long participantId, String meetingCode) {
        MeetingParticipant participant = participantEntityService
                .validateCancelableWaiting(participantId, meetingCode);
        if (participant == null) return null;
        participantEntityService.removeParticipant(participantId);
        return participant;
    }

    @Transactional
    public void endMeetingForEverybody() {

    }
}
