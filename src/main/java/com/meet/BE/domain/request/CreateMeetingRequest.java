package com.meet.BE.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingRequest {
    private String title;
//    private Boolean isScheduled;
//    private int emptyTimeout;
//    private int departureTimeout;
//    private int maxParticipants;
//    private String metadata;
}