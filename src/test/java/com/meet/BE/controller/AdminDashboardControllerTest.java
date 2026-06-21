package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.response.AdminDashboardOverviewResponseDTO;
import com.meet.BE.service.admin.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private AdminDashboardService adminDashboardService;

    @InjectMocks
    private AdminDashboardController adminDashboardController;

    @Test
    void shouldReturnOverviewResponseFromService() {
        AdminDashboardOverviewResponseDTO overview = new AdminDashboardOverviewResponseDTO(
                new AdminDashboardOverviewResponseDTO.MetricDTO(2543L, new BigDecimal("12.5"), "+12.5%", "up"),
                new AdminDashboardOverviewResponseDTO.MetricDTO(128L, new BigDecimal("23.1"), "+23.1%", "up"),
                new AdminDashboardOverviewResponseDTO.MetricDTO(847L, new BigDecimal("-5.2"), "-5.2%", "down")
        );
        ApiResponse<AdminDashboardOverviewResponseDTO> serviceResponse = new ApiResponse<>(
                overview,
                "Fetched dashboard overview successfully",
                null,
                200
        );

        when(adminDashboardService.getOverview()).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<AdminDashboardOverviewResponseDTO>> response =
                adminDashboardController.getOverview();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(adminDashboardService).getOverview();
    }
}
