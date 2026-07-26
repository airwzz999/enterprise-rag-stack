package com.knowledge.base.userauth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.userauth.dto.RegisterDTO;
import com.knowledge.base.userauth.dto.UserDTO;
import com.knowledge.base.userauth.entity.User;
import com.knowledge.base.userauth.vo.LoginVO;
import com.knowledge.base.userauth.vo.RegisterVO;
import com.knowledge.base.userauth.vo.UserStatisticsVO;
import com.knowledge.base.userauth.vo.UserVO;

import java.util.List;

/**
 * User Service interface
 *
 * <p>Designed following the Alibaba Java Development Guidelines; provides user business logic operations</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface UserService extends IService<User> {

    /**
     * User login
     *
     * @param username username
     * @param password password
     * @return login response information
     */
    LoginVO login(String username, String password);

    /**
     * Register
     *
     * @param registerDTO registration request parameters
     * @return registration response information (includes email verification status)
     */
    RegisterVO register(RegisterDTO registerDTO);

    /**
     * Verify email to activate the account
     *
     * @param token activation token
     * @return activation result message
     */
    String verifyEmail(String token);

    /**
     * User logout
     *
     * @param token token
     */
    void logout(String token);

    /**
     * Check whether a token is blacklisted (logged out)
     *
     * @param rawToken raw JWT token (without the Bearer prefix)
     * @return true if logged out/invalid, false if valid
     */
    boolean isTokenBlacklisted(String rawToken);

    /**
     * Query a user by username
     *
     * @param username username
     * @return user information
     */
    User getByUsername(String username);

    /**
     * Create a user
     *
     * @param userDTO user information
     * @return user ID
     */
    Long createUser(UserDTO userDTO);

    /**
     * Update a user
     *
     * @param userDTO user information
     * @return whether successful
     */
    Boolean updateUser(UserDTO userDTO);

    /**
     * Delete a user
     *
     * @param userId user ID
     * @return whether successful
     */
    Boolean deleteUser(Long userId);

    /**
     * Query a user by ID
     *
     * @param userId user ID
     * @return user information
     */
    UserVO getUserById(Long userId);

    /**
     * Paginated user query
     *
     * @param current  current page
     * @param size     page size
     * @param keyword  search keyword
     * @param role     role filter
     * @param status   status filter
     * @return paginated user information
     */
    IPage<UserVO> pageUsers(Long current, Long size, String keyword, String role, Integer status);

    /**
     * Reset a user's password
     *
     * @param userId          user ID
     * @param newPassword new password
     * @return whether successful
     */
    Boolean resetPassword(Long userId, String newPassword);

    /**
     * Change a user's password
     *
     * @param oldPassword old password
     * @param newPassword new password
     * @return whether successful
     */
    Boolean changePassword(String oldPassword, String newPassword);

    /**
     * Get the current logged-in user's information
     *
     * @return user information
     */
    UserVO getCurrentUserInfo();

    /**
     * Assign roles to a user
     *
     * @param userId  user ID
     * @param roleIds role ID list
     * @return whether successful
     */
    Boolean assignRoles(Long userId, List<Long> roleIds);

    /**
     * Get a user's assigned roles
     *
     * @param userId user ID
     * @return role ID list
     */
    List<Long> getUserRoles(Long userId);

    /**
     * Assign permissions to a user (directly, not via a role)
     *
     * @param userId        user ID
     * @param permissionIds permission ID list
     * @return whether successful
     */
    Boolean assignPermissions(Long userId, List<Long> permissionIds);

    /**
     * Get all of a user's permissions (including role-based and directly assigned permissions)
     *
     * @param userId user ID
     * @return permission code list
     */
    List<String> getUserPermissions(Long userId);

    /**
     * Refresh token
     *
     * @param refreshToken refresh token
     * @return new login response information (includes new access token and refresh token)
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * Get a user's statistics (document count, views, likes, comments)
     *
     * @param userId user ID
     * @return user statistics
     */
    UserStatisticsVO getUserStatistics(Long userId);

    /**
     * Send a password reset verification code
     *
     * @param email registered email
     */
    void sendResetCode(String email);

    /**
     * Verify the password reset code
     *
     * @param email registered email
     * @param code  verification code
     * @return verification result
     */
    boolean verifyResetCode(String email, String code);

    /**
     * Reset password
     *
     * @param email       registered email
     * @param code        verification code
     * @param newPassword new password
     */
    void resetPassword(String email, String code, String newPassword);

    /**
     * Validate a token and return the user identity information
     *
     * @param authorization Authorization request header (Bearer token)
     * @param token         token from URL parameter (fallback)
     * @return token validation result
     */
    com.knowledge.base.userauth.vo.TokenValidateVO validateToken(String authorization, String token);

    /**
     * Query user IDs by role code
     *
     * @param roleCode role code, e.g. ROLE_REVIEWER
     * @return user ID list
     */
    List<Long> getUserIdsByRoleCode(String roleCode);
}
