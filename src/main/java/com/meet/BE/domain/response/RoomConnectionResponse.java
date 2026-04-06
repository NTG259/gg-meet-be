package com.meet.BE.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RoomConnectionResponse {
    private String roomCode;
    private String participantName;
    private String token;
    private String wsUrl;
}
