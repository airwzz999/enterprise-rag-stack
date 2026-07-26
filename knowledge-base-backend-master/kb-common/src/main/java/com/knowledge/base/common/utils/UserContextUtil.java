package com.knowledge.base.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * User context utility class
 *
 * <p>Provides functionality to get and set the user context</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class UserContextUtil {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> AVATAR_HOLDER = new ThreadLocal<>();

    /**
     * Set the current user ID
     *
     * @param userId the user ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * Get the current user ID
     *
     * @return the user ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * Set the current username
     *
     * @param username the username
     */
    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    /**
     * Get the current username
     *
     * @return the username
     */
    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    /**
     * Set the token
     *
     * @param token the token
     */
    public static void setToken(String token) {
        TOKEN_HOLDER.set(token);
    }

    /**
     * Get the token
     *
     * @return the token
     */
    public static String getToken() {
        return TOKEN_HOLDER.get();
    }

    /**
     * Set the current user's avatar
     *
     * @param avatar the avatar URL
     */
    public static void setAvatar(String avatar) {
        AVATAR_HOLDER.set(avatar);
    }

    /**
     * Get the current user's avatar
     *
     * @return the avatar URL
     */
    public static String getAvatar() {
        return AVATAR_HOLDER.get();
    }

    /**
     * Get the user ID from the request
     *
     * @param request the HttpServletRequest
     * @return the user ID
     */
    public static Long getUserIdFromRequest(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return null;
        }

        try {
            // Parse the token using JwtTokenUtil
            JwtTokenUtil jwtTokenUtil = SpringContextUtil.getBean(JwtTokenUtil.class);
            return jwtTokenUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract the token from the request
     *
     * @param request the HttpServletRequest
     * @return the token
     */
    public static String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Clear the current user context
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
        TOKEN_HOLDER.remove();
        AVATAR_HOLDER.remove();
    }

    /**
     * Check whether the user is logged in
     *
     * @return whether the user is logged in
     */
    public static boolean isLoggedIn() {
        return getUserId() != null;
    }

    /**
     * Get the user ID from the X-User-Id request header (injected by the gateway)
     *
     * <p>Prefers reading the logged-in user from ThreadLocal, falling back to parsing the request header</p>
     *
     * @param request the HttpServletRequest
     * @return the user ID, or 1L (default user) if not found
     */
    public static Long getUserIdFromHeader(HttpServletRequest request) {
        Long threadLocalUserId = getUserId();
        if (threadLocalUserId != null) {
            return threadLocalUserId;
        }
        String userId = request.getHeader("X-User-Id");
        if (userId != null) {
            return Long.parseLong(userId);
        }
        return 1L;
    }
}
