package com.meet.BE.domain.response;

import com.meet.BE.domain.enums.ParticipantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingRoomParticipantResponseDTO {
    private Long participantId;
    private String name;
    private String email;
    private ParticipantStatus participantStatus;
    private String requestedAt;
}
