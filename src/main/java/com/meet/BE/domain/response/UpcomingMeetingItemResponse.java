package com.meet.BE.domain.response;

import com.meet.BE.domain.enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingMeetingItemResponse {
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String hostName;
    private String meetingCode;
    private MeetingStatus status;
}
