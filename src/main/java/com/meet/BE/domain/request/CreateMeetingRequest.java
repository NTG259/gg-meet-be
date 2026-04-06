package com.meet.BE.domain.request;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateMeetingRequest {
    private String participantName;
}