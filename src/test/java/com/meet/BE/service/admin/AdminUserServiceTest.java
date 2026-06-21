package com.meet.BE.service.admin;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.response.AdminUserResponseDTO;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void shouldBuildPagedUsersWithFiltersAndMeetingCounts() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 15, 10, 0);

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setFullName("Nguyen Van A");
        user.setRole("USER");
        user.setEnabled(true);
        user.setAvatar("https://example.com/avatar.png");
        user.setCreatedAt(createdAt);
        user.setIsDeleted(false);

        when(userRepository.findForAdmin(
                eq("nguyen"),
                eq("USER"),
                eq(true),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(user), PageRequest.of(2, 15), 31));
        when(meetingRepository.countMeetingsByHostIds(List.of(1L)))
                .thenReturn(List.of(hostMeetingCount(1L, 7L)));

        ApiResponse<PagedResponse<AdminUserResponseDTO>> response =
                adminUserService.getUsers(2, 15, " nguyen ", "user", "active");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findForAdmin(
                eq("nguyen"),
                eq("USER"),
                eq(true),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(15);
        assertThat(response.message()).isEqualTo("Fetched users successfully");
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data().page()).isEqualTo(2);
        assertThat(response.data().size()).isEqualTo(15);
        assertThat(response.data().totalElements()).isEqualTo(31);
        assertThat(response.data().totalPages()).isEqualTo(3);
        assertThat(response.data().last()).isTrue();

        AdminUserResponseDTO dto = response.data().content().get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getEmail()).isEqualTo("user@example.com");
        assertThat(dto.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(dto.getRole()).isEqualTo("USER");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        assertThat(dto.getAvatar()).isEqualTo("https://example.com/avatar.png");
        assertThat(dto.getMeetingCount()).isEqualTo(7L);
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldRejectInvalidRole() {
        assertThatThrownBy(() -> adminUserService.getUsers(0, 10, null, "MODERATOR", null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROLE);

        verifyNoInteractions(userRepository, meetingRepository);
    }

    @Test
    void shouldRejectInvalidStatus() {
        assertThatThrownBy(() -> adminUserService.getUsers(0, 10, null, "ADMIN", "BLOCKED"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS);

        verifyNoInteractions(userRepository, meetingRepository);
    }

    private MeetingRepository.HostMeetingCount hostMeetingCount(Long hostId, Long count) {
        return new MeetingRepository.HostMeetingCount() {
            @Override
            public Long getHostId() {
                return hostId;
            }

            @Override
            public Long getMeetingCount() {
                return count;
            }
        };
    }
}
