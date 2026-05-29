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
public class AdminMeetingResponseDTO {
    private Long id;
    private String meetingCode;
    private String title;
    private String status;
    private Boolean isScheduled;
    private Long hostId;
    private String hostName;
    private String hostEmail;
    private Long participantCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
}
