package com.vladyslavplus.lokora.security;

import tools.jackson.databind.ObjectMapper;
import com.vladyslavplus.lokora.common.api.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Clock;

public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final Clock clock;
    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(Clock clock, ObjectMapper objectMapper) {
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiErrorResponse(
                        "UNAUTHORIZED",
                        "Authentication is required",
                        clock.instant(),
                        request.getRequestURI()
                )
        );
    }
}
