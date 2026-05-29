package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.request.AdminCreateUserRequest;
import com.meet.BE.domain.request.AdminUpdateUserRequest;
import com.meet.BE.domain.response.AdminUserResponseDTO;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.service.admin.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserResponseDTO>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminUserService.getUsers(page, size, search, role, status));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponseDTO>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.getUserById(userId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserResponseDTO>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponseDTO>> updateUser(
            @PathVariable Long userId,
            @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(adminUserService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.deleteUser(userId));
    }
}
