package com.meet.BE.domain.dto;

public record MeetingTokenPrincipal(
        Long participantId,
        String meetingCode,
        String role
) {
}
