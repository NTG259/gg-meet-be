package com.meet.BE.domain.response;

import com.meet.BE.domain.enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingMeetingItemResponse {
    private String title;
    private String startTime;
    private String endTime;
    private String hostName;
    private String meetingCode;
    private MeetingStatus status;
}
