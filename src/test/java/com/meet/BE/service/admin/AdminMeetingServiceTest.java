package com.meet.BE.service.admin;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.response.AdminMeetingResponseDTO;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;

    @InjectMocks
    private AdminMeetingService adminMeetingService;

    @Test
    void shouldBuildPagedMeetingResponseWithFiltersAndParticipantCounts() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 31, 23, 59);
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 29, 9, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 28, 15, 30);

        User host = new User();
        host.setId(3L);
        host.setFullName("Nguyen Van A");
        host.setEmail("host@example.com");

        Meeting meeting = new Meeting();
        meeting.setId(12L);
        meeting.setMeetingCode("abc-defg-hij");
        meeting.setTitle("Daily standup");
        meeting.setStatus(MeetingStatus.ACTIVE);
        meeting.setIsScheduled(true);
        meeting.setHost(host);
        meeting.setStartTime(startTime);
        meeting.setEndTime(null);
        meeting.setCreatedAt(createdAt);

        when(meetingRepository.findForAdmin(
                eq(MeetingStatus.ACTIVE),
                eq("standup"),
                eq(from),
                eq(to),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(meeting), PageRequest.of(1, 20), 21));
        when(meetingParticipantRepository.countAcceptedParticipantsByMeetingIds(List.of(12L)))
                .thenReturn(List.of(acceptedParticipantCount(12L, 5L)));

        ApiResponse<PagedResponse<AdminMeetingResponseDTO>> response =
                adminMeetingService.getMeetings(1, 20, " standup ", "active", from, to);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingRepository).findForAdmin(
                eq(MeetingStatus.ACTIVE),
                eq("standup"),
                eq(from),
                eq(to),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();

        assertThat(response.message()).isEqualTo("Fetched meetings successfully");
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data().page()).isEqualTo(1);
        assertThat(response.data().size()).isEqualTo(20);
        assertThat(response.data().totalElements()).isEqualTo(21);
        assertThat(response.data().totalPages()).isEqualTo(2);
        assertThat(response.data().last()).isTrue();
        assertThat(response.data().content()).hasSize(1);

        AdminMeetingResponseDTO dto = response.data().content().get(0);
        assertThat(dto.getId()).isEqualTo(12L);
        assertThat(dto.getMeetingCode()).isEqualTo("abc-defg-hij");
        assertThat(dto.getTitle()).isEqualTo("Daily standup");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        assertThat(dto.getIsScheduled()).isTrue();
        assertThat(dto.getHostId()).isEqualTo(3L);
        assertThat(dto.getHostName()).isEqualTo("Nguyen Van A");
        assertThat(dto.getHostEmail()).isEqualTo("host@example.com");
        assertThat(dto.getParticipantCount()).isEqualTo(5L);
        assertThat(dto.getStartTime()).isEqualTo(startTime);
        assertThat(dto.getEndTime()).isNull();
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldDefaultParticipantCountsToZeroWhenPageIsEmpty() {
        when(meetingRepository.findForAdmin(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        ApiResponse<PagedResponse<AdminMeetingResponseDTO>> response =
                adminMeetingService.getMeetings(0, 10, null, null, null, null);

        assertThat(response.data().content()).isEmpty();
        verify(meetingParticipantRepository, never()).countAcceptedParticipantsByMeetingIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldRejectInvalidStatus() {
        assertThatThrownBy(() -> adminMeetingService.getMeetings(0, 10, null, "UNKNOWN", null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS);

        verifyNoInteractions(meetingRepository, meetingParticipantRepository);
    }

    private MeetingParticipantRepository.AcceptedParticipantCount acceptedParticipantCount(Long meetingId, Long count) {
        return new MeetingParticipantRepository.AcceptedParticipantCount() {
            @Override
            public Long getMeetingId() {
                return meetingId;
            }

            @Override
            public Long getParticipantCount() {
                return count;
            }
        };
    }
}
