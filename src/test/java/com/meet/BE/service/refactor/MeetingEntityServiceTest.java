package com.meet.BE.service.refactor;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.domain.response.UpcomingMeetingItemResponse;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingEntityServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private MeetingEntityService meetingEntityService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBuildPagedUpcomingMeetingResponse() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        User currentUser = new User();
        currentUser.setId(99L);
        currentUser.setEmail("user@example.com");
        currentUser.setFullName("Current User");

        User host = new User();
        host.setId(1L);
        host.setEmail("host@example.com");
        host.setFullName("Host Name");

        Meeting meeting = new Meeting();
        meeting.setHost(host);
        meeting.setMeetingCode("ROOM-123");
        meeting.setTitle("Sprint Planning");
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setStartTime(LocalDateTime.of(2026, 4, 21, 14, 0));
        meeting.setEndTime(LocalDateTime.of(2026, 4, 21, 15, 0));

        when(userService.handleFindUserByEmail("user@example.com")).thenReturn(currentUser);
        when(meetingRepository.findUpcomingMeetingsForUser(eq(99L), eq("user@example.com"), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(meeting), PageRequest.of(0, 10), 1));

        PagedResponse<UpcomingMeetingItemResponse> response = meetingEntityService.getUpcomingMeetings(0, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingRepository).findUpcomingMeetingsForUser(eq(99L), eq("user@example.com"), any(LocalDateTime.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("startTime")).isNotNull();
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).getTitle()).isEqualTo("Sprint Planning");
        assertThat(response.content().get(0).getHostName()).isEqualTo("Host Name");
        assertThat(response.content().get(0).getMeetingCode()).isEqualTo("ROOM-123");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.last()).isTrue();
    }
}
