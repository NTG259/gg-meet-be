package com.meet.BE.service.admin;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.request.AdminCreateUserRequest;
import com.meet.BE.domain.request.AdminUpdateUserRequest;
import com.meet.BE.domain.response.AdminUserResponseDTO;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.repository.UserRepository;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import com.meet.BE.utils.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiResponse<PagedResponse<AdminUserResponseDTO>> getUsers(
            int page,
            int size,
            String search,
            String role,
            String status
    ) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedSearch = normalizeSearch(search);
        String normalizedRole = parseRole(role);
        Boolean enabled = parseStatus(status);

        Page<User> userPage = userRepository.findForAdmin(normalizedSearch, normalizedRole, enabled, pageable);
        Map<Long, Long> meetingCounts = getHostMeetingCounts(userPage.getContent());

        List<AdminUserResponseDTO> content = userPage.getContent().stream()
                .map(user -> toAdminResponseDTO(user, meetingCounts.getOrDefault(user.getId(), 0L)))
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

        return new ApiResponse<>(
                toAdminResponseDTO(user, meetingRepository.countByHostId(user.getId())),
                "User details fetched",
                null,
                HttpStatus.OK.value()
        );
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
        return new ApiResponse<>(toAdminResponseDTO(saved, 0L), "User created successfully", null, HttpStatus.CREATED.value());
    }

    @Transactional
    public ApiResponse<AdminUserResponseDTO> updateUser(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        String newRole = request.getRole().toUpperCase();
        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(newRole)) {
            assertNotLastAdmin(user);
        }

        user.setRole(newRole);

        User updated = userRepository.save(user);
        return new ApiResponse<>(
                toAdminResponseDTO(updated, meetingRepository.countByHostId(updated.getId())),
                "User updated successfully",
                null,
                HttpStatus.OK.value()
        );
    }

    @Transactional
    public ApiResponse<AdminUserResponseDTO> banUser(Long id) {
        User caller = getCurrentAdmin();

        if (caller.getId().equals(id)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        User user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new BusinessException(ErrorCode.USER_BANNED);
        }

        if ("ADMIN".equals(user.getRole())) {
            assertNotLastAdmin(user);
        }

        user.setIsBanned(true);
        Long currentVersion = user.getAccessTokenVersion() == null ? 0L : user.getAccessTokenVersion();
        user.setAccessTokenVersion(currentVersion + 1);
        user.setRefreshToken(null);

        User updated = userRepository.save(user);
        return new ApiResponse<>(
                toAdminResponseDTO(updated, meetingRepository.countByHostId(updated.getId())),
                "User banned successfully",
                null,
                HttpStatus.OK.value()
        );
    }

    @Transactional
    public ApiResponse<AdminUserResponseDTO> unbanUser(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getIsBanned())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        user.setIsBanned(false);

        User updated = userRepository.save(user);
        return new ApiResponse<>(
                toAdminResponseDTO(updated, meetingRepository.countByHostId(updated.getId())),
                "User unbanned successfully",
                null,
                HttpStatus.OK.value()
        );
    }

    @Transactional
    public ApiResponse<Void> deleteUser(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if ("ADMIN".equals(user.getRole())) {
            assertNotLastAdmin(user);
        }

        user.setIsDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);

        return new ApiResponse<>(null, "User deleted (deactivated) successfully", null, HttpStatus.OK.value());
    }

    private User getCurrentAdmin() {
        String email = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void assertNotLastAdmin(User target) {
        if (!"ADMIN".equals(target.getRole())) return;
        if (userRepository.countActiveAdmins() <= 1) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private String parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (!"USER".equals(normalizedRole) && !"ADMIN".equals(normalizedRole)) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }
        return normalizedRole;
    }

    private Boolean parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> true;
            case "INACTIVE" -> false;
            default -> throw new BusinessException(ErrorCode.INVALID_STATUS);
        };
    }

    private Map<Long, Long> getHostMeetingCounts(List<User> users) {
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return meetingRepository.countMeetingsByHostIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        MeetingRepository.HostMeetingCount::getHostId,
                        MeetingRepository.HostMeetingCount::getMeetingCount,
                        Long::sum
                ));
    }

    private AdminUserResponseDTO toAdminResponseDTO(User user, Long meetingCount) {
        AdminUserResponseDTO dto = new AdminUserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setStatus(Boolean.TRUE.equals(user.getEnabled()) ? "ACTIVE" : "INACTIVE");
        dto.setIsBanned(Boolean.TRUE.equals(user.getIsBanned()));
        dto.setAvatar(user.getAvatar());
        dto.setMeetingCount(meetingCount);
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
