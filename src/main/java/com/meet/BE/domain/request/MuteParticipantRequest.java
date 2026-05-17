package com.meet.BE.domain.request;

import com.meet.BE.domain.enums.TrackType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MuteParticipantRequest {

    @NotNull
    private TrackType trackType;
}
