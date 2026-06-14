package com.meet.BE.service.admin;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.response.AdminMeetingResponseDTO;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.service.user.LiveKitService;
import com.meet.BE.service.user.NotificationService;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final LiveKitService liveKitService;
    private final NotificationService notificationService;

    public ApiResponse<PagedResponse<AdminMeetingResponseDTO>> getMeetings(
            int page,
            int size,
            String search,
            String status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        MeetingStatus meetingStatus = parseMeetingStatus(status);
        String normalizedSearch = normalizeSearch(search);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Meeting> meetingPage = meetingRepository.findForAdmin(
                meetingStatus,
                normalizedSearch,
                from,
                to,
                pageable
        );
        Map<Long, Long> participantCounts = getAcceptedParticipantCounts(meetingPage.getContent());

        List<AdminMeetingResponseDTO> content = meetingPage.getContent().stream()
                .map(meeting -> toAdminResponseDTO(meeting, participantCounts.getOrDefault(meeting.getId(), 0L)))
                .toList();

        PagedResponse<AdminMeetingResponseDTO> pagedResponse = new PagedResponse<>(
                content,
                meetingPage.getNumber(),
                meetingPage.getSize(),
                meetingPage.getTotalElements(),
                meetingPage.getTotalPages(),
                meetingPage.isLast()
        );

        return new ApiResponse<>(pagedResponse, "Fetched meetings successfully", null, HttpStatus.OK.value());
    }

    private MeetingStatus parseMeetingStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return MeetingStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private Map<Long, Long> getAcceptedParticipantCounts(List<Meeting> meetings) {
        List<Long> meetingIds = meetings.stream()
                .map(Meeting::getId)
                .toList();

        if (meetingIds.isEmpty()) {
            return Map.of();
        }

        return meetingParticipantRepository.countAcceptedParticipantsByMeetingIds(meetingIds)
                .stream()
                .collect(Collectors.toMap(
                        MeetingParticipantRepository.AcceptedParticipantCount::getMeetingId,
                        MeetingParticipantRepository.AcceptedParticipantCount::getParticipantCount,
                        Long::sum
                ));
    }

    @Transactional
    public ApiResponse<Void> forceEndMeeting(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEETING_ALREADY_ENDED);
        }

        liveKitService.deleteRoom(meeting.getMeetingCode());

        meeting.setStatus(MeetingStatus.ENDED);
        meeting.setEndTime(LocalDateTime.now());
        meetingRepository.save(meeting);

        notificationService.sendMeetingEnded(meeting.getMeetingCode());

        return new ApiResponse<>(null, "Meeting force-ended successfully", null, HttpStatus.OK.value());
    }

    private AdminMeetingResponseDTO toAdminResponseDTO(Meeting meeting, Long participantCount) {
        User host = meeting.getHost();

        AdminMeetingResponseDTO dto = new AdminMeetingResponseDTO();
        dto.setId(meeting.getId());
        dto.setMeetingCode(meeting.getMeetingCode());
        dto.setTitle(meeting.getTitle());
        dto.setStatus(meeting.getStatus() == null ? null : meeting.getStatus().name());
        dto.setIsScheduled(meeting.getIsScheduled());
        dto.setHostId(host == null ? null : host.getId());
        dto.setHostName(host == null ? null : host.getFullName());
        dto.setHostEmail(host == null ? null : host.getEmail());
        dto.setParticipantCount(participantCount);
        dto.setStartTime(meeting.getStartTime());
        dto.setEndTime(meeting.getEndTime());
        dto.setCreatedAt(meeting.getCreatedAt());
        return dto;
    }
}
