package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.response.AdminMeetingResponseDTO;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.service.admin.AdminMeetingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMeetingControllerTest {

    @Mock
    private AdminMeetingService adminMeetingService;

    @InjectMocks
    private AdminMeetingController adminMeetingController;

    @Test
    void shouldReturnMeetingsResponseFromService() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 31, 23, 59);
        PagedResponse<AdminMeetingResponseDTO> pagedResponse = new PagedResponse<>(
                List.of(),
                0,
                10,
                0,
                0,
                true
        );
        ApiResponse<PagedResponse<AdminMeetingResponseDTO>> serviceResponse = new ApiResponse<>(
                pagedResponse,
                "Fetched meetings successfully",
                null,
                200
        );

        when(adminMeetingService.getMeetings(0, 10, "daily", "ACTIVE", from, to))
                .thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<PagedResponse<AdminMeetingResponseDTO>>> response =
                adminMeetingController.getMeetings(0, 10, "daily", "ACTIVE", from, to);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(adminMeetingService).getMeetings(0, 10, "daily", "ACTIVE", from, to);
    }
}
