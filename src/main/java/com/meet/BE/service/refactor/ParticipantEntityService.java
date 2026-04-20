package com.meet.BE.service.refactor;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParticipantEntityService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRepository meetingRepository;

    // Hàm này chỉ tạo người tham gia. Vì host đã được tạo cùng lúc tạo phòng rồi
    public MeetingParticipant createUserParticipant(Meeting meeting, User user, ParticipantStatus status) {
        MeetingParticipant p = new MeetingParticipant();
        p.setMeeting(meeting);
        p.setUser(user);
        p.setRole(Role.PARTICIPANT);
        p.setParticipantStatus(status);
        return meetingParticipantRepository.save(p);
    }

    public MeetingParticipant createGuestParticipant(Meeting meeting, GuestJoinRequest guest) {
        MeetingParticipant p = new MeetingParticipant();
        p.setMeeting(meeting);
        p.setGuestId(guest.getGuestId());
        p.setGuestName(guest.getGuestName());
        p.setRole(Role.PARTICIPANT);
        p.setParticipantStatus(ParticipantStatus.WAITING);
        return meetingParticipantRepository.save(p);
    }

    public void saveInvitedUser(Meeting meeting, List<String> invitedEmails) {
        for (String email : invitedEmails) {
            MeetingParticipant participant = new MeetingParticipant();
            participant.setUser(null);
            participant.setEmail(email);
            participant.setParticipantStatus(ParticipantStatus.INVITED);
            participant.setRole(Role.PARTICIPANT);
            participant.setMeeting(meeting);
            this.meetingParticipantRepository.save(participant);
        }
    }

    public MeetingParticipant updateParticipantStatus(MeetingParticipant participant, ParticipantStatus status) {
        participant.setParticipantStatus(status);
        return meetingParticipantRepository.save(participant);
    }

    public Optional<MeetingParticipant> findExistingUserParticipant(Meeting meeting, User user) {
        return meetingParticipantRepository
                .findByMeetingIdAndUserId(meeting.getId(), user.getId());
    }

    public Optional<MeetingParticipant> findInvitedUser(Meeting meeting, String email) {
        return meetingParticipantRepository
                .findByMeetingIdAndEmail(meeting.getId(), email);
    }
    public Optional<MeetingParticipant> findExistingGuestParticipant(Meeting meeting, GuestJoinRequest guestRequest) {
        return meetingParticipantRepository
                .findByMeetingIdAndGuestId(meeting.getId(), guestRequest.getGuestId());
    }

    public MeetingParticipant handleParticipantStatus(
            MeetingParticipant existing
    ) {
        ParticipantStatus status = existing.getParticipantStatus();

        // Nếu người dùng bị block
        if (status == ParticipantStatus.BLOCKED) {
            throw new BusinessException(ErrorCode.USER_BANNED_FROM_MEETING);
        }

        // Điều kiện cho usecase join thẳng nếu người dùng đã tham gia trước đó hoặc được mời
        if (status == ParticipantStatus.ACCEPT || status == ParticipantStatus.INVITED) {
            existing.setParticipantStatus(ParticipantStatus.ACCEPT);
            return meetingParticipantRepository.save(existing);
        }

        // Điều kiện cho usecase join phòng chờ
        if (status == ParticipantStatus.REJECT || status == ParticipantStatus.LEFT) {
            existing.setParticipantStatus(ParticipantStatus.WAITING);
            return meetingParticipantRepository.save(existing);
        }

        return existing;
    }

    public void removeParticipant(Long participantId) {
        if (this.meetingParticipantRepository.existsById(participantId)) {
            this.meetingParticipantRepository.deleteById(participantId);
        }
    }

    public MeetingParticipant validateCancelableWaiting(Long participantId, String meetingCode) {
        MeetingParticipant p = meetingParticipantRepository.findById(participantId)
                .orElse(null);

        if (p == null) return null;

        if (!p.getMeeting().getMeetingCode().equals(meetingCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_IN_THIS_ROOM);
        }

        if (p.getParticipantStatus() != ParticipantStatus.WAITING) {
            return null;
        }

        return p;
    }

    public MeetingParticipant validateParticipantInMeeting(Long participantId, String meetingCode) {
        if (participantId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        MeetingParticipant participant = meetingParticipantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!participant.getMeeting().getMeetingCode().equals(meetingCode)) {
            throw new BusinessException(ErrorCode.NOT_PERMIT);
        }

        return participant;
    }

    public MeetingParticipant getParticipantInMeeting(Long participantId, String meetingCode) {
        if (participantId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Meeting meeting = this.meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        return this.meetingParticipantRepository.findByMeetingIdAndParticipantId(meeting.getId(), participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_IN_THIS_ROOM));
    }

    public MeetingParticipant getUserInMeeting(Long userId, String meetingCode) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Meeting meeting = this.meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        return this.meetingParticipantRepository.findByMeetingIdAndUserId(meeting.getId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_IN_THIS_ROOM));
    }

}
