package com.meet.BE.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {
    @NotBlank(message = "test")
    private String username;
    @NotBlank(message = "test")
    private String password;
}