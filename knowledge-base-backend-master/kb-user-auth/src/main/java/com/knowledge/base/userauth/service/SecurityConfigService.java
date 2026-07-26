package com.knowledge.base.userauth.service;

/**
 * Security configuration service interface
 *
 * <p>Reads security-related configuration from the kb_system_config table, used by
 * the login, registration, and other flows</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface SecurityConfigService {

    /**
     * Get the minimum password length
     */
    int getPasswordMinLength();

    /**
     * Whether a special character is required
     */
    boolean isRequireSpecialChar();

    /**
     * Get the password policy level (low/medium/high)
     */
    String getPasswordPolicy();

    /**
     * Get the session timeout (seconds)
     */
    long getSessionTimeout();

    /**
     * Get the maximum number of login attempts
     */
    int getLoginMaxRetry();

    /**
     * Whether IP restriction is enabled
     */
    boolean isIpRestrictionEnabled();

    /**
     * Whether two-factor authentication is enabled
     */
    boolean is2FAEnabled();

    /**
     * Get the value of a specific config item
     *
     * @param configKey config key
     * @return config value, or null if not present
     */
    String getConfig(String configKey);

    /**
     * Validate whether a password meets the policy
     *
     * @param password plaintext password
     * @throws com.knowledge.base.common.exception.BusinessException thrown when the password does not meet the policy
     */
    void validatePassword(String password);

    /**
     * Record a login failure
     *
     * @param username username
     * @return remaining attempts (0 means locked)
     */
    int recordLoginFailure(String username);

    /**
     * Clear login failure records
     *
     * @param username username
     */
    void clearLoginFailure(String username);

    /**
     * Check whether an account is locked
     *
     * @param username username
     * @return true if locked
     */
    boolean isAccountLocked(String username);
}
