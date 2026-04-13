package com.meet.BE.domain.message;

import com.meet.BE.domain.enums.ParticipantStatus;
import lombok.Data;

@Data
public class MeetingMessage {
    private String meetingCode;
    private Long userId;
    private String name;
    private ParticipantStatus status;
}

