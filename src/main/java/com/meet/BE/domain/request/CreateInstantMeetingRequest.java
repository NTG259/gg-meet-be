package com.meet.BE.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstantMeetingRequest {
    private String title;

    private Integer emptyTimeout = 20;
    private Integer departureTimeout = 300;
    private Integer maxParticipants = 20;

    public CreateInstantMeetingRequest(String title) {
        this.title = title;
    }
}