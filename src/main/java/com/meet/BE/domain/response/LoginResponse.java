package com.meet.BE.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    @JsonProperty("access_token")
    private String accessToken;

    // Only populated for non-browser clients (X-Client: desktop/mobile), which
    // cannot use the HTTP-only cookie. Stays out of the web response body.
    @JsonProperty("refresh_token")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String refreshToken;

    private UserLogin user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLogin{
        private Long id;
        private String email;
        private String fullName;
        private String avatar;
        private String role;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGetAccount {
        private UserLogin user;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInsideToken{
        private Long id;
        private String email;
        private String fullName;
        private String avatar;
        private String role;
    }
}
