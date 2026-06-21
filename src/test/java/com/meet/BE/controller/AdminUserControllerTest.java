package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.response.AdminUserResponseDTO;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.service.admin.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void shouldPassFiltersToUserService() {
        PagedResponse<AdminUserResponseDTO> pagedResponse = new PagedResponse<>(
                List.of(),
                0,
                10,
                0,
                0,
                true
        );
        ApiResponse<PagedResponse<AdminUserResponseDTO>> serviceResponse = new ApiResponse<>(
                pagedResponse,
                "Fetched users successfully",
                null,
                200
        );

        when(adminUserService.getUsers(0, 10, "nguyen", "USER", "ACTIVE"))
                .thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<PagedResponse<AdminUserResponseDTO>>> response =
                adminUserController.getUsers(0, 10, "nguyen", "USER", "ACTIVE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(adminUserService).getUsers(0, 10, "nguyen", "USER", "ACTIVE");
    }
}
