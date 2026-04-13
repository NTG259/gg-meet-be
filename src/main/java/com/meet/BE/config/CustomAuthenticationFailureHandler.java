package com.meet.BE.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        String message = "Login failed";

        if (exception instanceof DisabledException) {
            message = "Tài khoản đã bị vô hiệu hóa";
        } else if (exception instanceof BadCredentialsException) {
            message = "Sai email hoặc mật khẩu";
        } else if (exception instanceof LockedException) {
            message = "Tài khoản đã bị khóa";
        }

        response.getWriter().write("""
            {
                "error": "%s"
            }
        """.formatted(message));
    }
}