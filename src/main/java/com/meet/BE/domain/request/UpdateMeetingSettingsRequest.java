package com.meet.BE.domain.request;

import lombok.Data;

@Data
public class UpdateMeetingSettingsRequest {
    private Boolean allowParticipantShareScreen;
    private Boolean allowParticipantUnmute;
}
