package com.meet.BE.service.user;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantServiceRF {
    private final ParticipantEntityService participantEntityService;

    @Transactional
    public MeetingParticipant handleAuthenticatedJoin(Meeting meeting, User user) {
        if (meeting.getHost() != null && meeting.getHost().getId().equals(user.getId())) {
            log.info(
                    "Handling authenticated host join: meetingId={}, meetingCode={}, userId={}",
                    meeting.getId(),
                    meeting.getMeetingCode(),
                    user.getId()
            );
            return this.participantEntityService.ensureHostParticipant(meeting, user);
        }

        Optional<MeetingParticipant> existingOpt =
                participantEntityService.findExistingUserParticipant(meeting, user);
        if (existingOpt.isPresent()) {
            log.info(
                    "Existing authenticated participant found: participantId={}, status={}",
                    existingOpt.get().getParticipantId(),
                    existingOpt.get().getParticipantStatus()
            );
            return participantEntityService.handleParticipantStatus(existingOpt.get());
        }

        return handleInvitedJoin(meeting, user.getEmail(), user, null);
    }

    @Transactional
    public MeetingParticipant handleGuestJoin(Meeting meeting, GuestJoinRequest guestRequest) {
        validateGuestRequest(guestRequest);

        log.info(
                "Handling guest join: meetingId={}, meetingCode={}, guestId={}, guestName={}",
                meeting.getId(),
                meeting.getMeetingCode(),
                guestRequest != null ? guestRequest.getGuestId() : null,
                guestRequest != null ? guestRequest.getGuestName() : null
        );
        // Cho chức năng Guest
        Optional<MeetingParticipant> existingOpt = this.participantEntityService
                .findExistingGuestParticipant(meeting, guestRequest);
        // Kiểm tra người này đã vào phòng lần nào chưa
        if (existingOpt.isPresent()) {
            if (!isGuestSecretValid(existingOpt.get(), guestRequest)) {
                throw new BusinessException(ErrorCode.NOT_PERMIT);
            }
            log.info(
                    "Existing guest participant found: participantId={}, status={}",
                    existingOpt.get().getParticipantId(),
                    existingOpt.get().getParticipantStatus()
            );
            return this.participantEntityService.handleParticipantStatus(existingOpt.get());
        } else {
            MeetingParticipant created = this.participantEntityService.createGuestParticipant(meeting, guestRequest);
            log.info(
                    "Created waiting guest participant: participantId={}, status={}",
                    created.getParticipantId(),
                    created.getParticipantStatus()
            );
            return created;
        }
    }

    public MeetingParticipant handleInvitedJoin(Meeting meeting, String email, User user, GuestJoinRequest guest) {
        Optional<MeetingParticipant> invited = this.participantEntityService.findInvitedUser(meeting, email);
        log.info(
                "Handling invited join lookup: meetingId={}, meetingCode={}, email={}, invitedFound={}, userId={}",
                meeting.getId(),
                meeting.getMeetingCode(),
                email,
                invited.isPresent(),
                user != null ? user.getId() : null
        );

        if (invited.isPresent()) {
            MeetingParticipant participant = invited.get();
            if (user != null) {
                participant.setUser(user);
            } else {
                participant.setGuestId(guest.getGuestId());
                participant.setGuestName(guest.getGuestName());
                participant.setGuestSecret(UUID.randomUUID().toString());
            }
            log.info(
                    "Invited participant matched: participantId={}, previousStatus={}",
                    participant.getParticipantId(),
                    participant.getParticipantStatus()
            );
            return this.participantEntityService.handleParticipantStatus(participant);
        } else {
            if (user != null) {
                log.info("No invited participant found, falling back to authenticated waiting flow: userId={}", user.getId());
                return handleUserJoin(meeting, user);
            } else {
                log.info("No invited participant found, falling back to guest waiting flow");
                return handleGuestJoin(meeting, guest);
            }
        }
    }

    public MeetingParticipant handleUserJoin(Meeting meeting, User user) {
        Optional<MeetingParticipant> existingOpt =
                participantEntityService.findExistingUserParticipant(meeting, user);
        log.info(
                "Handling user join: meetingId={}, meetingCode={}, userId={}, existingFound={}",
                meeting.getId(),
                meeting.getMeetingCode(),
                user.getId(),
                existingOpt.isPresent()
        );
        // Nếu đã từng vào
        if (existingOpt.isPresent()) {
            log.info(
                    "Existing user participant found: participantId={}, status={}",
                    existingOpt.get().getParticipantId(),
                    existingOpt.get().getParticipantStatus()
            );
            return participantEntityService.handleParticipantStatus(existingOpt.get());
        }
        // Nếu user chưa từng vào thì phải waiting
        MeetingParticipant created = participantEntityService.createUserParticipant(meeting, user, ParticipantStatus.WAITING);
        log.info(
                "Created waiting user participant: participantId={}, userId={}, status={}",
                created.getParticipantId(),
                user.getId(),
                created.getParticipantStatus()
        );
        return created;
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
        return participantEntityService.updateParticipantStatus(participant, ParticipantStatus.LEFT);
    }

    @Transactional
    public void endMeetingForEverybody() {

    }

    @Transactional
    public MeetingParticipant kickParticipant(Long targetParticipantId, String meetingCode, Boolean isBan) {
        MeetingParticipant participant = participantEntityService.validateParticipantInMeeting(targetParticipantId, meetingCode);
        ParticipantStatus status = isBan ? ParticipantStatus.BLOCK : ParticipantStatus.LEFT;
        return participantEntityService.updateParticipantStatus(participant, status);
    }

    private void validateGuestRequest(GuestJoinRequest guestRequest) {
        if (guestRequest == null
                || guestRequest.getGuestId() == null
                || guestRequest.getGuestId().isBlank()
                || guestRequest.getGuestName() == null
                || guestRequest.getGuestName().isBlank()) {
            throw new BusinessException(ErrorCode.DATA_NOT_ENOUGH);
        }
    }

    private boolean isGuestSecretValid(MeetingParticipant participant, GuestJoinRequest guestRequest) {
        return participant.getGuestSecret() != null
                && guestRequest.getGuestSecret() != null
                && participant.getGuestSecret().equals(guestRequest.getGuestSecret());
    }
}
