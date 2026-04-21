package com.meet.BE.service.refactor;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.request.ScheduleMeetingRequest;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.domain.response.UpcomingMeetingItemResponse;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import com.meet.BE.utils.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingEntityService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final UserService userService;

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

    public Meeting createScheduleMeetingEntity(User host, ScheduleMeetingRequest request) {
        String meetingCode = UUID.randomUUID().toString().substring(0, 5);
        Meeting meeting = new Meeting();
        meeting.setHost(host);
        meeting.setMeetingCode(meetingCode);
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setStartTime(LocalDateTime.of(request.getMeetingDate(), request.getMeetingTime()));
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

    public Meeting validateMeetingBeforeJoin(String meetingCode) {
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

    public Meeting findByMeetingId(Long meetingId) {
        return this.meetingRepository.findById(meetingId).orElseThrow(
                () -> new BusinessException(ErrorCode.ROOM_NOT_FOUND)
        );
    }

    public Meeting validateMeetingBeforeDelete(String meetingCode) {
        Meeting meeting = findByMeetingCode(meetingCode);
        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEETING_ALREADY_ENDED);
        }
        return meeting;
    }

    public Meeting updateMeetingStatus(Meeting meeting, MeetingStatus status) {
        meeting.setStatus(status);
        return this.meetingRepository.save(meeting);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UpcomingMeetingItemResponse> getUpcomingMeetings(int page, int size) {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startTime"));
        Page<Meeting> meetingPage = this.meetingRepository.findUpcomingMeetingsForUser(
                currentUser.getId(),
                currentUser.getEmail(),
                LocalDateTime.now(),
                pageable
        );

        return new PagedResponse<>(
                meetingPage.getContent().stream()
                        .map(this::mapUpcomingMeeting)
                        .toList(),
                meetingPage.getNumber(),
                meetingPage.getSize(),
                meetingPage.getTotalElements(),
                meetingPage.getTotalPages(),
                meetingPage.isLast()
        );
    }

    private UpcomingMeetingItemResponse mapUpcomingMeeting(Meeting meeting) {
        return new UpcomingMeetingItemResponse(
                meeting.getTitle(),
                meeting.getStartTime(),
                meeting.getEndTime(),
                meeting.getHost().getFullName(),
                meeting.getMeetingCode(),
                meeting.getStatus()
        );
    }
}
