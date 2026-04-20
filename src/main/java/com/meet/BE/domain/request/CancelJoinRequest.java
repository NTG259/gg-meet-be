package com.meet.BE.domain.request;

import lombok.Data;

@Data
public class CancelJoinRequest {
    private String meetingToken;
    private Long targetParticipantId;
}
