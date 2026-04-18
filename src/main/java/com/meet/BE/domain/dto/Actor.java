package com.meet.BE.domain.dto;

import com.meet.BE.domain.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Actor {
    private User user; // Sẽ là null nếu là Guest
    private String guestId;
    private String displayName;
    private String email;

    public boolean isRegisteredUser() {
        return user != null;
    }
}