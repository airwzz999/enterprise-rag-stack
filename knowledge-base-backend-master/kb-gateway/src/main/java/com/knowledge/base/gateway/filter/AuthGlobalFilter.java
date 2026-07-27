package com.knowledge.base.gateway.filter;

import com.knowledge.base.common.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * Global authentication filter
 *
 * <p>Parses the JWT token at the gateway layer and passes the userId to downstream
 * microservices via the X-User-Id request header, so downstream services don't need
 * to re-parse the token.</p>
 *
 * <p>Downstream services trust the X-User-Id header as proof of identity, so this
 * filter must (a) reject any request on a non-whitelisted path that doesn't carry a
 * valid token, and (b) strip any client-supplied X-User-Id on every path, so it can
 * never be forwarded unverified.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Value("${gateway.white-list:}")
    private List<String> whiteList;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Never trust a client-supplied X-User-Id; strip it before any further
        // processing so it can only be (re)established below from a verified token.
        ServerHttpRequest.Builder builder = request.mutate();
        builder.headers(headers -> headers.remove(USER_ID_HEADER));

        if ("OPTIONS".equals(request.getMethod().name()) || isWhitelisted(path)) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        String token = extractToken(request);
        if (token == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication token"));
        }

        Long userId;
        try {
            userId = jwtTokenUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.debug("AuthGlobalFilter: token parse failed for path={}: {}", path, e.getMessage());
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
        }

        if (userId == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
        }

        builder.header(USER_ID_HEADER, String.valueOf(userId));
        ServerHttpRequest modifiedRequest = builder.build();
        log.debug("AuthGlobalFilter: token parsed OK, userId={}, path={}", userId, path);
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isWhitelisted(String path) {
        if (whiteList == null) {
            return false;
        }
        for (String pattern : whiteList) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
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
