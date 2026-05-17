package com.meet.BE.domain.message;

import lombok.Data;

import java.util.Map;

@Data
public class MeetingWsMessage {
    private String meetingCode;
    private Long targetParticipantId;
    private String targetName;
    private String action;
    private Boolean isBan;
    private Map<String, Object> extra;
}