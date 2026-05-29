package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.response.AdminDashboardOverviewResponseDTO;
import com.meet.BE.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponseDTO>> getOverview() {
        return ResponseEntity.ok(adminDashboardService.getOverview());
    }
}
