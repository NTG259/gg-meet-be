package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.domain.response.UpcomingMeetingItemResponse;
import com.meet.BE.service.user.MeetingEntityService;
import com.meet.BE.service.user.MeetingOrchestratorService;
import com.meet.BE.service.user.SecurityService;
import com.meet.BE.service.user.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingControllerTest {

    @Mock
    private MeetingEntityService meetingEntityService;

    @Mock
    private MeetingOrchestratorService meetingOrchestratorService;

    @Mock
    private SecurityService securityService;

    @Mock
    private UserService userService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private MeetingController meetingController;

    @Test
    void shouldReturnUpcomingMeetingsResponseWrappedInApiResponse() {
        PagedResponse<UpcomingMeetingItemResponse> serviceResponse = new PagedResponse<>(
                List.of(new UpcomingMeetingItemResponse(
                        "Planning",
                        "2026-04-21T14:00",
                        "2026-04-21T15:00",
                        "Host Name",
                        "ROOM-1",
                        com.meet.BE.domain.enums.MeetingStatus.SCHEDULED
                )),
                0,
                10,
                1,
                1,
                true
        );
        when(meetingEntityService.getUpcomingMeetings(0, 10)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<PagedResponse<UpcomingMeetingItemResponse>>> response =
                meetingController.getUpcomingMeetings(0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(serviceResponse);
        assertThat(response.getBody().message()).isEqualTo("Upcoming meetings fetched successfully");
        assertThat(response.getBody().status()).isEqualTo(200);
    }
}
