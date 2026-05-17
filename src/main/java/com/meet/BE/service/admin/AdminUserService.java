package com.meet.BE.service.admin;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.request.AdminCreateUserRequest;
import com.meet.BE.domain.request.AdminUpdateUserRequest;
import com.meet.BE.domain.response.AdminUserResponseDTO;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.repository.UserRepository;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiResponse<PagedResponse<AdminUserResponseDTO>> getUsers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;

        if (search == null || search.isBlank()) {
            userPage = userRepository.findAllByIsDeletedFalse(pageable);
        } else {
            userPage = userRepository.searchUsers(search.trim(), pageable);
        }

        List<AdminUserResponseDTO> content = userPage.getContent().stream()
                .map(this::toAdminResponseDTO)
                .toList();

        PagedResponse<AdminUserResponseDTO> pagedResponse = new PagedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );

        return new ApiResponse<>(pagedResponse, "Fetched users successfully", null, HttpStatus.OK.value());
    }

    public ApiResponse<AdminUserResponseDTO> getUserById(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new ApiResponse<>(toAdminResponseDTO(user), "User details fetched", null, HttpStatus.OK.value());
    }

    @Transactional
    public ApiResponse<AdminUserResponseDTO> createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXIST);
        }

        User user = new User();
        user.setEmail(request.getEmail().trim());
        user.setFullName(request.getFullName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null && !request.getRole().isBlank() ? request.getRole().toUpperCase() : "USER");
        user.setEnabled(true);
        user.setIsDeleted(false);

        User saved = userRepository.save(user);
        return new ApiResponse<>(toAdminResponseDTO(saved), "User created successfully", null, HttpStatus.CREATED.value());
    }

    @Transactional
    public ApiResponse<AdminUserResponseDTO> updateUser(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole().toUpperCase());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            switch (request.getStatus().toUpperCase()) {
                case "ACTIVE" -> user.setEnabled(true);
                case "INACTIVE" -> user.setEnabled(false);
                default -> throw new BusinessException(ErrorCode.INVALID_STATUS);
            }
        }

        User updated = userRepository.save(user);
        return new ApiResponse<>(toAdminResponseDTO(updated), "User updated successfully", null, HttpStatus.OK.value());
    }

    @Transactional
    public ApiResponse<Void> deleteUser(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setIsDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);

        return new ApiResponse<>(null, "User deleted (deactivated) successfully", null, HttpStatus.OK.value());
    }

    private AdminUserResponseDTO toAdminResponseDTO(User user) {
        AdminUserResponseDTO dto = new AdminUserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setStatus(Boolean.TRUE.equals(user.getEnabled()) ? "ACTIVE" : "INACTIVE");
        dto.setAvatar(user.getAvatar());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
