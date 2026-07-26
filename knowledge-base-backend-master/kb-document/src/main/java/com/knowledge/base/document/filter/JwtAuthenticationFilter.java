package com.knowledge.base.document.filter;

import com.knowledge.base.common.utils.JwtTokenUtil;
import com.knowledge.base.common.utils.UserContextUtil;
import com.knowledge.base.document.client.UserServiceClient;
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
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT authentication filter (dedicated to the document service)
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Parse the JWT token from the request header to verify the user's identity</li>
 *   <li>Set the user information into the Spring Security context</li>
 *   <li>Set the user information into the ThreadLocal context for subsequent business use</li>
 *   <li>Clear the context after the request completes to prevent memory leaks</li>
 * </ul>
 *
 * <p>Differences from the user authentication service:</p>
 * <ul>
 *   <li>Does not depend on UserMapper, parses user information directly from the token</li>
 *   <li>Suitable for business services within a microservices architecture</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Resource
    private UserServiceClient userServiceClient;

    @Value("${security.jwt.exclude-paths}")
    private String[] excludePaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * JWT token authentication processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        log.debug("JwtAuthenticationFilter: URI={}, Method={}", requestUri, method);

        // Extract the token from the request
        String token = extractTokenFromRequest(request);
        log.info("JwtAuthenticationFilter processing request: uri={}, method={}, hasToken={}", requestUri, method, token != null);

        boolean authenticated = false;

        if (token != null) {
            try {
                // Parse the token to get the user information
                Long userId = parseToken(token);
                if (userId != null) {
                    log.info("Token parsed successfully: userId={}, uri={}", userId, requestUri);
                    setSecurityContext(userId, token, request);
                    UserContextUtil.setUserId(userId);
                    UserContextUtil.setToken(token);
                    // Extract username and avatar from the JWT
                    String username = jwtTokenUtil.getUsernameFromToken(token);
                    if (username != null) {
                        UserContextUtil.setUsername(username);
                    }
                    String avatar = jwtTokenUtil.getAvatarFromToken(token);
                    if (avatar != null) {
                        UserContextUtil.setAvatar(avatar);
                    }
                    authenticated = true;
                } else {
                    log.warn("JWT token is invalid or expired: uri={}, method={}", requestUri, method);
                }
            } catch (Exception e) {
                log.error("JWT authentication exception: uri={}, method={}, error={}", requestUri, method, e.getMessage(), e);
            }
        } else {
            log.warn("Request did not include a token: uri={}, method={}", requestUri, method);
        }

        // Fallback: X-User-Id header (set by gateway after JWT validation)
        if (!authenticated) {
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null) {
                try {
                    Long userId = Long.parseLong(userIdHeader);
                    log.info("Authenticating via X-User-Id header: userId={}, uri={}", userId, requestUri);
                    // The gateway fallback authentication must also populate the SecurityContext,
                    // otherwise anyRequest().authenticated() will treat the request as unauthenticated.
                    setSecurityContext(userId, token, request);

                    UserContextUtil.setUserId(userId);
                    if (token != null) {
                        UserContextUtil.setToken(token);
                    }
                    authenticated = true;
                } catch (NumberFormatException e) {
                    log.warn("Invalid X-User-Id header format: {}", userIdHeader);
                }
            }
        }

        try {
            // Continue the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Clear the context after the request completes to prevent ThreadLocal memory leaks
            UserContextUtil.clear();
        }
    }

    /**
     * Sets the Spring Security authentication context
     */
    private void setSecurityContext(Long userId, String token, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = getUserAuthorities(userId, token);

        // Create the authentication object
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,      // principal: user ID
                        token,       // credentials: keep the token for tracing
                        authorities  // authorities: permission list
                );

        // Set the authentication details
        WebAuthenticationDetails details = new WebAuthenticationDetailsSource().buildDetails(request);
        authentication.setDetails(details);

        // Set the authentication object into the Spring Security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Fallback strategy when the permission service is unreachable: grants only basic read permissions.
     * <p>Non-admin users should not receive sensitive permissions such as edit/delete.</p>
     */
    private static final List<String> FALLBACK_BASIC_PERMISSIONS = List.of(
            "document:list",
            "document:category:query"
    );

    /**
     * Gets the user's permission list
     *
     * <p>Fallback strategy (two layers):</p>
     * <ol>
     *   <li>Permission list is empty (kb-user-auth unreachable/erroring) → grant all document permissions</li>
     *   <li>Permission list is non-empty but missing {@code document:list} (incomplete database permission mapping)
     *   → supplement with basic document permissions</li>
     * </ol>
     */
    private List<SimpleGrantedAuthority> getUserAuthorities(Long userId, String token) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        // Add the default normal-user role
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        List<String> permissions = userServiceClient.getUserPermissions(userId, token);

        log.info("User permission query result: userId={}, permissionCount={}, permissions={}", userId, permissions.size(), permissions);

        if (permissions.isEmpty()) {
            log.warn("Could not fetch permissions from the user service, using fallback strategy: granting only basic read permissions, userId={}", userId);
            permissions = FALLBACK_BASIC_PERMISSIONS;
        } else if (!permissions.contains("document:list")) {
            // kb-user-auth returned permissions, but the basic document view permission is missing (incomplete database permission mapping)
            log.warn("User permissions are missing document:list, auto-supplementing basic permissions, userId={}", userId);
            List<String> augmented = new ArrayList<>(permissions);
            augmented.add("document:list");
            augmented.add("document:category:query");
            permissions = augmented;
        }

        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return authorities;
    }

    /**
     * Parses the token to get the user ID
     *
     * <p>Parses the token directly to extract the userId, skipping expiration validation
     * (expiration is handled by the frontend refreshing the token).</p>
     */
    private Long parseToken(String token) {
        return jwtTokenUtil.getUserIdFromToken(token);
    }

    /**
     * Extracts the token from the request header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Prefer reading from the Authorization header
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // Fallback: read from a cookie (for requests such as <video>/<audio>/fetch() that don't go through axios)
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    String cookieToken = cookie.getValue();
                    if (cookieToken != null && !cookieToken.isBlank()) {
                        return cookieToken;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Determines whether this filter should be skipped
     *
     * <p>Excludes paths configured via {@code security.jwt.exclude-paths},
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
