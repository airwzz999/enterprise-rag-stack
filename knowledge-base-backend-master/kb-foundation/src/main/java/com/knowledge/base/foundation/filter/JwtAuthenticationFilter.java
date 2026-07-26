package com.knowledge.base.foundation.filter;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import com.knowledge.base.foundation.dto.TokenValidateVO;
import com.knowledge.base.foundation.feign.UserAuthFeignClient;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JWT authentication filter (dedicated to the foundation service)
 *
 * <p>Delegates JWT token validation to the kb-user-auth microservice (via a Feign
 * call), centralizing the authentication logic and avoiding duplicate JWT parsing
 * and role lookups across microservices.</p>
 *
 * <p>Authentication flow:</p>
 * <ol>
 *   <li>Extract the Bearer token from the request header</li>
 *   <li>Validate the token via a Feign call to kb-user-auth's /auth/validate</li>
 *   <li>kb-user-auth is responsible for: JWT parsing, blacklist checks, user info lookup, role lookup</li>
 *   <li>This filter sets the Spring Security context and ThreadLocal based on the returned result</li>
 * </ol>
 *
 * <p>Relationship with WebSocket authentication:</p>
 * <ul>
 *   <li>REST API requests → handled by this filter (Feign call to kb-user-auth to validate the token)</li>
 *   <li>WebSocket /ws/** → bypasses this filter (validated at the STOMP layer by WebSocketAuthInterceptor)</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private UserAuthFeignClient userAuthFeignClient;

    @Value("${security.jwt.exclude-paths}")
    private String[] excludePaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String bearerToken = request.getHeader("Authorization");
        boolean authenticated = false;

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            try {
                // === Validate the token via a Feign call to kb-user-auth ===
                Result<TokenValidateVO> result = userAuthFeignClient
                        .validateToken(bearerToken, null);
                TokenValidateVO validateVO = result != null ? result.getData() : null;

                if (validateVO != null && Boolean.TRUE.equals(validateVO.getValid())) {
                    log.debug("Token validation succeeded: userId={}, roles={}, uri={}",
                            validateVO.getUserId(), validateVO.getRoles(), requestUri);

                    setSecurityContext(validateVO, bearerToken, request);
                    UserContextUtil.setUserId(validateVO.getUserId());

                    if (validateVO.getUsername() != null) {
                        UserContextUtil.setUsername(validateVO.getUsername());
                    }
                    if (validateVO.getAvatar() != null) {
                        UserContextUtil.setAvatar(validateVO.getAvatar());
                    }
                    authenticated = true;
                } else {
                    log.debug("Token validation failed: token is invalid or expired, uri={}", requestUri);
                }
            } catch (Exception e) {
                log.error("Feign call to kb-user-auth to validate the token threw an exception: uri={}, error={}",
                        requestUri, e.getMessage());
                // When Feign falls back, valid=false, so the fallback logic below kicks in
            }
        }

        // Fallback: X-User-Id header (set by the gateway after it has already validated the JWT, as a fallback)
        if (!authenticated) {
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdHeader);
                    log.debug("Authenticating via the X-User-Id header: userId={}, uri={}", userId, requestUri);
                    UserContextUtil.setUserId(userId);
                    if (bearerToken != null) {
                        UserContextUtil.setToken(bearerToken);
                    }
                    // Requests authenticated via the X-User-Id header are granted the default ROLE_USER
                    setDefaultSecurityContext(userId, bearerToken, request);
                } catch (NumberFormatException e) {
                    log.warn("Invalid X-User-Id header format: {}", userIdHeader);
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextUtil.clear();
        }
    }

    /**
     * Set the Spring Security context based on the token validation result returned by Feign
     */
    private void setSecurityContext(TokenValidateVO validateVO, String token,
                                     HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        List<String> roles = validateVO.getRoles();
        if (roles != null) {
            for (String role : roles) {
                // Spring Security requires roles to start with ROLE_
                String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                authorities.add(new SimpleGrantedAuthority(roleWithPrefix));
            }
        }
        // Fallback: ensure there is at least one role
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        validateVO.getUserId(), token, authorities);
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Set the default Security context (used for the X-User-Id header fallback scenario)
     */
    private void setDefaultSecurityContext(Long userId, String token,
                                            HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, token,
                        Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_USER")));
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Skip paths that do not require authentication
     *
     * <p>Excluded paths are configured via {@code security.jwt.exclude-paths},
     * supporting Ant-style path matching.</p>
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        for (String pattern : excludePaths) {
            if (pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }
}
