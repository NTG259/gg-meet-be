package com.meet.BE.domain.response;
import com.meet.BE.domain.enums.MeetingStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MeetingResponseDTO {
    private UserResponseDTO host;
    private String meetingCode;
    private String roomSid;
    private String title;
    private MeetingStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String livekitToken;
}