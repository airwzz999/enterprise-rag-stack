package com.knowledge.base.document.utils;

import com.knowledge.base.common.utils.UserContextUtil;

/**
 * User context utility class
 *
 * <p>Fetches the current logged-in user's information from ThreadLocal</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class UserContext {

    /**
     * Gets the current user ID
     *
     * @return user ID
     * @throws IllegalStateException if the user is not logged in (should throw in production)
     */
    public static Long getCurrentUserId() {
        Long userId = UserContextUtil.getUserId();
        if (userId == null) {
            // Throw if not available from the context (should not happen while logged in)
            throw new IllegalStateException("User is not logged in or the session has expired, please log in again");
        }
        return userId;
    }

    /**
     * Gets the current username
     *
     * @return username
     */
    public static String getCurrentUserName() {
        String username = UserContextUtil.getUsername();
        if (username == null) {
            throw new IllegalStateException("User login information is incomplete, please log in again");
        }
        return username;
    }

    /**
     * Gets the current user's token
     *
     * @return token
     */
    public static String getCurrentToken() {
        return UserContextUtil.getToken();
    }

    /**
     * Gets the current user's avatar
     *
     * @return avatar URL
     */
    public static String getCurrentUserAvatar() {
        return UserContextUtil.getAvatar();
    }
}
