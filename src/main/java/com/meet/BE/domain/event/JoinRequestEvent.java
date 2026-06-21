package com.meet.BE.domain.event;

public record JoinRequestEvent(String meetingCode, Long participantId, String participantName) {}
