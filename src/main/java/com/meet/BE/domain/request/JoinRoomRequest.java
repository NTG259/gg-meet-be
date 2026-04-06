package com.meet.BE.domain.request;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JoinRoomRequest {
    private String participantName;
    private String roomCode;
}