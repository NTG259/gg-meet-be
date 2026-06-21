package com.meet.BE.utils.convert;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.domain.response.UserResponseDTO;

import java.time.LocalDateTime;

public class MeetingConvert {
    public static MeetingResponseDTO ToResponse(Meeting meeting, String liveKitToken) {
        MeetingResponseDTO response = new MeetingResponseDTO();
        UserResponseDTO host = new UserResponseDTO();
        host.setId(meeting.getHost().getId());
        host.setFullName(meeting.getHost().getFullName());
        host.setEmail(meeting.getHost().getEmail());
        host.setAvatar(meeting.getHost().getAvatar());
        response.setHost(host);
        response.setStatus(meeting.getStatus());
        response.setTitle(meeting.getTitle());
        response.setMeetingCode(meeting.getMeetingCode());
        response.setRoomSid(meeting.getRoomSid());
        response.setStartTime(formatLocalDateTime(meeting.getStartTime()));
        response.setEndTime(formatLocalDateTime(meeting.getEndTime()));
        response.setLivekitToken(liveKitToken);
        return response;
    }

    private static String formatLocalDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
