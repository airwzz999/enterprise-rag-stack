package com.knowledge.base.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.base.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Custom access-denied handler
 *
 * <p>Triggered when a logged-in user accesses a resource they don't have permission for (e.g. missing role/permission).</p>
 * <p>Returns a unified JSON response with HTTP status 403.</p>
 * <p>Note: the token is not cleared and re-login is not required; it only indicates insufficient permissions.</p>
 * <p>Only active in Servlet-based web applications; WebFlux applications such as the Gateway do not create this bean.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied due to insufficient permissions: {} {}, user: {}, exception: {}",
                request.getMethod(), request.getRequestURI(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "unknown",
                accessDeniedException.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Void> result = Result.error(HttpStatus.FORBIDDEN.value(), "Insufficient permissions, access denied");
        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(result));
        writer.flush();
    }
}
