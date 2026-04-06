package com.meet.BE.utils.convert;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.domain.response.UserResponseDTO;

public class MeetingConvert {
    public static MeetingResponseDTO ToResponse(Meeting meeting, String liveKitToken) {
        MeetingResponseDTO response = new MeetingResponseDTO();
        UserResponseDTO host = new UserResponseDTO();
        host.setFullName(meeting.getHost().getFullName());
        host.setEmail(meeting.getHost().getEmail());
        response.setHost(host);
        response.setStatus(meeting.getStatus());
        response.setTitle(meeting.getTitle());
        response.setMeetingCode(meeting.getMeetingCode());
        response.setStartTime(meeting.getStartTime());
        response.setEndTime(meeting.getEndTime());
        response.setLivekitToken(liveKitToken);
        return response;
    }
}
