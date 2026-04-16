package com.meet.BE.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingRequest {
    private String title;
    private Boolean isScheduled;
    private Integer emptyTimeout;
    private Integer departureTimeout;
    private Integer maxParticipants;
    private String metadata;

    public CreateMeetingRequest(String title) {
        this.title = title;
        emptyTimeout = 300;
        departureTimeout = 100000000;
        maxParticipants = 20;
    }
}