package com.securepay.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns the error envelope with HTTP 401 instead of Spring's default on unauthenticated access
 * (doc 07 §7.1, doc 08 §8.4). Message mirrors the source {@code Unauthenticated.}.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ResponseFactory responseFactory;
    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ResponseFactory responseFactory, ObjectMapper objectMapper) {
        this.responseFactory = responseFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var entity = responseFactory.error("Unauthenticated.", "", HttpStatus.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(entity.getBody()));
    }
}
