package com.meet.BE.domain.response;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyMeetingResponseDTO {
    private String meetingCode;
    private String title;
    private MeetingStatus status;
    private UserResponseDTO host;

    public static VerifyMeetingResponseDTO from(Meeting meeting) {
        UserResponseDTO host = new UserResponseDTO(
                meeting.getHost().getId(),
                meeting.getHost().getFullName(),
                meeting.getHost().getEmail(),
                meeting.getHost().getAvatar()
        );
        return new VerifyMeetingResponseDTO(
                meeting.getMeetingCode(),
                meeting.getTitle(),
                meeting.getStatus(),
                host
        );
    }
}
