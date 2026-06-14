package com.meet.BE.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserResponseDTO {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private Boolean isBanned;
    private String avatar;
    private Long meetingCount;
    private LocalDateTime createdAt;
}
