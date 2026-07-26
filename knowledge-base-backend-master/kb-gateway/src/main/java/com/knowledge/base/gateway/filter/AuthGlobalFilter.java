package com.knowledge.base.gateway.filter;

import com.knowledge.base.common.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.annotation.Resource;

/**
 * Global authentication filter
 *
 * <p>Parses the JWT token at the gateway layer and passes the userId to downstream
 * microservices via the X-User-Id request header, so downstream services don't need
 * to re-parse the token.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip public endpoints
        if (shouldSkip(path, request.getMethod().name())) {
            return chain.filter(exchange);
        }

        // Extract and parse the token
        String token = extractToken(request);
        if (token == null) {
            return chain.filter(exchange);
        }

        try {
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            if (userId != null) {
                // Inject the userId into the request header for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", String.valueOf(userId))
                        .build();
                log.debug("AuthGlobalFilter: token parsed OK, userId={}, path={}", userId, path);
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }
        } catch (Exception e) {
            log.debug("AuthGlobalFilter: token parse failed for path={}: {}", path, e.getMessage());
        }

        return chain.filter(exchange);
    }

    private boolean shouldSkip(String path, String method) {
        if ("OPTIONS".equals(method)) return true;
        return path.startsWith("/api/user/") ||
               path.startsWith("/auth/") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/ws/") ||
               path.startsWith("/doc.html") ||
               path.startsWith("/swagger-resources/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/webjars/");
    }

    private String extractToken(ServerHttpRequest request) {
        // Prefer reading from the Authorization header
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // Fallback: read from a cookie (for requests like <video>/<audio>/fetch() that bypass the interceptor)
        var cookies = request.getCookies();
        if (cookies.containsKey("access_token")) {
            var cookie = cookies.getFirst("access_token");
            if (cookie != null && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -200; // Runs before GlobalLogFilter
    }
}
