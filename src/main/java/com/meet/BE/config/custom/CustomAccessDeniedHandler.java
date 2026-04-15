package com.meet.BE.config.custom;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.utils.errors.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Object> res = new ApiResponse<>(
                null,
                ErrorCode.ACCESS_DENIED.getMessage(),
                ErrorCode.ACCESS_DENIED.name(),
                ErrorCode.ACCESS_DENIED.getStatus().value()
        );

        mapper.writeValue(response.getWriter(), res);
    }
}