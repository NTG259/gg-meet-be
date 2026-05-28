package com.meet.BE.domain.request;

import lombok.Data;

@Data
public class GuestJoinRequest {
    private String guestId;
    private String guestSecret;
    private String guestName;
}
