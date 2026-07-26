package com.knowledge.base.userauth.filter;

import cn.hutool.crypto.digest.DigestUtil;
import com.knowledge.base.common.utils.JwtTokenUtil;
import com.knowledge.base.common.utils.UserContextUtil;
import com.knowledge.base.userauth.entity.User;
import com.knowledge.base.userauth.mapper.RoleMapper;
import com.knowledge.base.userauth.mapper.UserMapper;
import com.knowledge.base.userauth.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JWT authentication filter
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Parse the JWT token from the request header and verify the user's identity</li>
 *   <li>Set the user information into the Spring Security context</li>
 *   <li>Set the user information into the ThreadLocal context for later use by business logic</li>
 *   <li>Support both real JWT tokens and mock tokens (for development/testing)</li>
 *   <li>Clear the context after the request completes to prevent memory leaks</li>
 * </ul>
 *
 * <p>Execution order: runs before the Spring Security filters</p>
 * <p>Exception handling: authentication failures do not block the request; whether authentication
 * is required is decided by the business layer</p>
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
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserService userService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Value("${security.jwt.exclude-paths}")
    private String[] excludePaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * JWT token authentication processing
     *
     * <p>Processing flow:</p>
     * <ol>
     *   <li>Extract the token from the request</li>
     *   <li>Validate the token</li>
     *   <li>Parse the user information and set it into the Spring Security context</li>
     *   <li>Parse the user information and set it into the ThreadLocal context</li>
     *   <li>Run the remaining filters</li>
     *   <li>Clear the context</li>
     * </ol>
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @param filterChain filter chain
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        log.info("========== JwtAuthenticationFilter start ==========");
        log.info("Request URI: {}, Method: {}", requestUri, method);

        // Extract the token from the request
        String token = extractTokenFromRequest(request);
        log.info("Extracted token: {}", token != null ? token.substring(0, Math.min(30, token.length())) + "..." : "null");

        if (token != null) {
            try {
                // Check whether the token is blacklisted (logged out)
                if (isTokenBlacklisted(token)) {
                    log.warn("Token is blacklisted (already logged out): uri={}", requestUri);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Token has expired, please log in again\"}");
                    return;
                }

                // Parse the token to get the user information
                UserInfo userInfo = parseToken(token);
                if (userInfo != null) {
                    log.info("Token parsed successfully: userId={}, username={}", userInfo.getUserId(), userInfo.getUsername());

                    // Set the Spring Security authentication context
                    setSecurityContext(userInfo, token, request);

                    // Set the custom ThreadLocal context
                    UserContextUtil.setUserId(userInfo.getUserId());
                    UserContextUtil.setUsername(userInfo.getUsername());
                    UserContextUtil.setToken(token);

                    log.info("JWT authentication succeeded; SecurityContext has been set");
                    log.info("Authentication in SecurityContext: {}", SecurityContextHolder.getContext().getAuthentication());
                } else {
                    log.warn("JWT token is invalid or expired: uri={}, method={}", requestUri, method);
                }
            } catch (Exception e) {
                log.error("JWT authentication error: uri={}, method={}, error={}", requestUri, method, e.getMessage(), e);
                // Authentication failure does not block the request; the business layer decides whether authentication is required
            }
        } else {
            log.warn("Request did not include a token: uri={}, method={}", requestUri, method);
        }

        try {
            log.info("Running the remaining filter chain...");
            // Run the remaining filters
            filterChain.doFilter(request, response);
            log.info("Remaining filter chain finished, response status: {}", response.getStatus());
        } finally {
            // Clear the context after the request completes, to prevent ThreadLocal memory leaks
            UserContextUtil.clear();
            log.info("========== JwtAuthenticationFilter end ==========");
        }
    }

    /**
     * Check whether the token is blacklisted (logged out)
     *
     * @param token raw token
     * @return true if blacklisted, false if normal
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            String tokenHash = DigestUtil.sha256Hex(token);
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_token_blacklist WHERE token_hash = ? AND expire_time > NOW()",
                Integer.class,
                tokenHash
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Failed to check token blacklist: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set the Spring Security authentication context
     *
     * <p>This is the key step for resolving 403 errors</p>
     * <p>Spring Security determines whether a user is authenticated via SecurityContextHolder</p>
     *
     * @param userInfo user information
     * @param token JWT token
     * @param request HTTP request
     */
    private void setSecurityContext(UserInfo userInfo, String token, HttpServletRequest request) {
        log.info("Setting SecurityContext...");

        List<SimpleGrantedAuthority> authorities = getUserAuthorities(userInfo);
        log.info("User authority list: {}", authorities);

        // Create the authentication object
        // Uses the three-argument constructor: principal, credentials, authorities
        // An object created this way is automatically marked as authenticated (authenticated=true)
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userInfo.getUserId(),  // principal: user ID
                        token,                // credentials: keep the token for tracing
                        authorities          // authorities: authority list
                );

        log.info("Created authentication object: principal={}, credentials length={}, authorities={}",
                userInfo.getUserId(), token.length(), authorities);

        // Set authentication details
        WebAuthenticationDetails details = new WebAuthenticationDetailsSource().buildDetails(request);
        authentication.setDetails(details);
        log.info("Set authentication details: remoteAddress={}, sessionId={}",
                details.getRemoteAddress(), details.getSessionId());

        // Set the authentication object into the Spring Security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("Authentication object has been set into SecurityContextHolder");

        // Verify the setting succeeded
        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Verifying SecurityContext: is null={}, isAuthenticated={}, principal={}, authorities={}, class={}",
                auth == null,
                auth != null && auth.isAuthenticated(),
                auth != null ? auth.getPrincipal() : null,
                auth != null ? auth.getAuthorities() : null,
                auth != null ? auth.getClass().getSimpleName() : null);
    }

    /**
     * Get the user's full authority list
     *
     * <p>Loads the user's role codes and permission codes from the database and builds a
     * Spring Security Authority list.</p>
     * <ol>
     *   <li>Role codes are converted to authorities with a ROLE_ prefix (e.g. ROLE_USER, ROLE_REVIEWER)</li>
     *   <li>Permission codes are used directly as authorities (e.g. document:list, system:user)</li>
     *   <li>If the permission list is empty, ROLE_USER is granted as a fallback</li>
     * </ol>
     *
     * @param userInfo user information
     * @return authority list
     */
    private List<SimpleGrantedAuthority> getUserAuthorities(UserInfo userInfo) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        try {
            // 1. Load the user's role codes from the database
            List<String> roleCodes = roleMapper.selectRoleCodesByUserId(userInfo.getUserId());
            if (roleCodes != null) {
                for (String roleCode : roleCodes) {
                    // Ensure the role has a ROLE_ prefix
                    String roleWithPrefix = roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode;
                    authorities.add(new SimpleGrantedAuthority(roleWithPrefix));
                }
            }

            // 2. Load the user's permission codes from the database (direct permissions + role-inherited permissions)
            List<String> permissions = userService.getUserPermissions(userInfo.getUserId());
            if (permissions != null) {
                for (String permission : permissions) {
                    authorities.add(new SimpleGrantedAuthority(permission));
                }
            }
        } catch (Exception e) {
            log.error("Failed to load user authorities: userId={}, error={}", userInfo.getUserId(), e.getMessage());
        }

        // 3. Fallback: ensure at least one role is present
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return authorities;
    }

    /**
     * Parse the token to get the user information
     *
     * <p>Supports two token formats:</p>
     * <ol>
     *   <li>Real JWT token: parsed using JwtTokenUtil</li>
     *   <li>Mock token: used for development/testing, in the format mock-access-token-{userId}</li>
     * </ol>
     *
     * @param token JWT token
     * @return user information, or null if parsing fails
     */
    private UserInfo parseToken(String token) {
        // First try to parse as a real JWT token
        if (jwtTokenUtil.validateToken(token)) {
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            if (userId != null) {
                return loadUserInfo(userId);
            }
        }

        // Development environment: support mock tokens
        if (token.startsWith("mock-access-token-")) {
            return parseMockToken(token);
        }

        return null;
    }

    /**
     * Load user information
     *
     * <p>Queries the database for detailed user information, avoiding a query on every request
     * would ideally be cached</p>
     * <p>Can be optimized with a cache implementation later</p>
     *
     * @param userId user ID
     * @return user information
     */
    private UserInfo loadUserInfo(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user != null && user.getStatus() == 1) {
                return UserInfo.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to query user information: userId={}", userId, e);
        }
        return null;
    }

    /**
     * Parse a mock token (for development/testing)
     *
     * @param token mock token
     * @return user information
     */
    private UserInfo parseMockToken(String token) {
        try {
            String userIdStr = token.substring("mock-access-token-".length());
            Long userId = Long.parseLong(userIdStr);
            return loadUserInfo(userId);
        } catch (NumberFormatException e) {
            log.error("Failed to parse mock token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract the token from the request header
     *
     * <p>Supports two formats:</p>
     * <ol>
     *   <li>Authorization: Bearer {token}</li>
     *   <li>Authorization: {token}</li>
     * </ol>
     *
     * @param request HTTP request
     * @return token string, or null if not present
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Prefer reading from the Authorization header
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // Fallback: read from a cookie (for native browser requests that bypass the axios interceptor)
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
     * Determine whether this filter should be skipped
     *
     * <p>Excludes paths configured via {@code security.jwt.exclude-paths},
     * supporting Ant-style path matching (e.g. {@code /public/**}).
     * OPTIONS preflight requests are always skipped.</p>
     *
     * @param request HTTP request
     * @return true to skip filtering, false to run it
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // OPTIONS preflight requests are always skipped
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

    /**
     * Internal user information class
     */
    @lombok.Data
    @lombok.Builder
    private static class UserInfo {
        private Long userId;
        private String username;
    }
}
