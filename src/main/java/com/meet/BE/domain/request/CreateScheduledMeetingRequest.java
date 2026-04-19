package com.meet.BE.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduledMeetingRequest {
    private String title;

    private LocalDateTime startTime;

    private Boolean requireApproval = true;
    private Boolean allowGuest = true;

    private List<String> invitedEmails;

    private Integer maxParticipants = 20;
}