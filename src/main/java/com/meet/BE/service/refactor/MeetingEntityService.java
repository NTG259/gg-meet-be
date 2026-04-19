package com.meet.BE.service.refactor;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MeetingEntityService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;

    public Meeting createInstantMeetingEntity(User host, String meetingCode, String title) {
        Meeting meeting = new Meeting();
        meeting.setHost(host);
        meeting.setMeetingCode(meetingCode);
        meeting.setTitle(title);
        meeting.setStatus(MeetingStatus.ACTIVE);
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setStartTime(LocalDateTime.now());
        meeting.setRoomSid(meetingCode);
        return meetingRepository.save(meeting);
    }

    public MeetingParticipant createHostParticipant(Meeting meeting, User host) {
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeeting(meeting);
        participant.setUser(host);
        participant.setRole(Role.HOST);
        participant.setParticipantStatus(ParticipantStatus.ACCEPT);
        return meetingParticipantRepository.save(participant);
    }

    public MeetingParticipant createUserParticipant(Meeting meeting, User user) {
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeeting(meeting);
        participant.setUser(user);
        participant.setRole(Role.PARTICIPANT);
        participant.setParticipantStatus(ParticipantStatus.WAITING);
        return meetingParticipantRepository.save(participant);
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

    public Meeting validateMeeting(String meetingCode) {
        Meeting meeting = findByMeetingCode(meetingCode);
        if (meeting.getStatus() == MeetingStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.MEETING_SCHEDULED_NOT_STARTED);
        } else if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new BusinessException(ErrorCode.MEETING_ALREADY_ENDED);
        }
        return meeting;
    }

    public Meeting findByMeetingCode(String meetingCode) {
        return this.meetingRepository.findByMeetingCode(meetingCode).orElseThrow(
                () -> new BusinessException(ErrorCode.ROOM_NOT_FOUND)
        );
    }
}
