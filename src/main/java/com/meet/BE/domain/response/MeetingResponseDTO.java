package com.meet.BE.domain.response;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.enums.ParticipantStatus;
import lombok.*;

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
    private String startTime;
    private String endTime;
    private String livekitToken;
    private String meetingToken;
    private String guestSecret;
    private ParticipantStatus participantStatus;
}
