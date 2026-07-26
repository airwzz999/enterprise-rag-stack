package com.knowledge.base.userauth.service.impl;

import com.knowledge.base.common.config.SystemConfigCache;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.userauth.service.SecurityConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Security configuration service implementation
 *
 * <p>Reads system configuration from the Redis cache, and provides password validation
 * and login failure counting</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class SecurityConfigServiceImpl implements SecurityConfigService {

    @Resource
    private SystemConfigCache systemConfigCache;

    /** Login failure records */
    private final ConcurrentHashMap<String, LoginFailRecord> failMap = new ConcurrentHashMap<>();

    private static final int DEFAULT_PASSWORD_MIN_LENGTH = 8;
    private static final long DEFAULT_SESSION_TIMEOUT = 3600L;
    private static final int DEFAULT_LOGIN_MAX_RETRY = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    // ==================== Configuration reads ====================

    @Override
    public int getPasswordMinLength() {
        String val = getConfig("auth.password.min.length");
        if (val != null) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_PASSWORD_MIN_LENGTH;
    }

    @Override
    public boolean isRequireSpecialChar() {
        String val = getConfig("auth.password.require.special");
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    @Override
    public String getPasswordPolicy() {
        String val = getConfig("system.passwordPolicy");
        return (val != null && !val.isEmpty()) ? val : "medium";
    }

    @Override
    public long getSessionTimeout() {
        String val = getConfig("auth.session.timeout");
        if (val != null) {
            try {
                return Long.parseLong(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_SESSION_TIMEOUT;
    }

    @Override
    public int getLoginMaxRetry() {
        String val = getConfig("auth.login.max.retry");
        if (val != null) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_LOGIN_MAX_RETRY;
    }

    @Override
    public boolean isIpRestrictionEnabled() {
        String val = getConfig("system.ipRestriction");
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    @Override
    public boolean is2FAEnabled() {
        String val = getConfig("system.enable2FA");
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    @Override
    public String getConfig(String configKey) {
        return systemConfigCache.getConfig(configKey);
    }

    // ==================== Password policy validation ====================

    @Override
    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException("Password must not be blank");
        }

        int minLength = getPasswordMinLength();
        boolean requireSpecial = isRequireSpecialChar();
        String policy = getPasswordPolicy();

        // Minimum length check
        if (password.length() < minLength) {
            throw new BusinessException("Password must be at least " + minLength + " characters long");
        }

        switch (policy) {
            case "low" -> validateLowPolicy(password, requireSpecial);
            case "medium" -> validateMediumPolicy(password, requireSpecial);
            case "high" -> validateHighPolicy(password, requireSpecial);
            default -> validateMediumPolicy(password, requireSpecial);
        }
    }

    private void validateLowPolicy(String password, boolean requireSpecial) {
        // low: only checks the minimum length and special character requirement
        if (requireSpecial && !containsSpecialChar(password)) {
            throw new BusinessException("Password must contain at least one special character (e.g. !@#$%^&*)");
        }
    }

    private void validateMediumPolicy(String password, boolean requireSpecial) {
        // medium: must contain both letters and digits
        if (!containsLetter(password) || !containsDigit(password)) {
            throw new BusinessException("Password must contain both letters and digits");
        }
        if (requireSpecial && !containsSpecialChar(password)) {
            throw new BusinessException("Password must contain at least one special character (e.g. !@#$%^&*)");
        }
    }

    private void validateHighPolicy(String password, boolean requireSpecial) {
        // high: must contain uppercase letters, lowercase letters, digits, and special characters
        if (!containsUpperCase(password)) {
            throw new BusinessException("Password must contain at least one uppercase letter");
        }
        if (!containsLowerCase(password)) {
            throw new BusinessException("Password must contain at least one lowercase letter");
        }
        if (!containsDigit(password)) {
            throw new BusinessException("Password must contain at least one digit");
        }
        if (!containsSpecialChar(password)) {
            throw new BusinessException("Password must contain at least one special character (e.g. !@#$%^&*)");
        }
    }

    private boolean containsDigit(String str) {
        return str.chars().anyMatch(Character::isDigit);
    }

    private boolean containsLetter(String str) {
        return str.chars().anyMatch(Character::isLetter);
    }

    private boolean containsUpperCase(String str) {
        return str.chars().anyMatch(c -> Character.isUpperCase(c));
    }

    private boolean containsLowerCase(String str) {
        return str.chars().anyMatch(c -> Character.isLowerCase(c));
    }

    private boolean containsSpecialChar(String str) {
        return str.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
    }

    // ==================== Login failure counting ====================

    @Override
    public int recordLoginFailure(String username) {
        int maxRetry = getLoginMaxRetry();
        LoginFailRecord record = failMap.compute(username, (k, v) -> {
            if (v == null || v.isExpired()) {
                return new LoginFailRecord(1, System.currentTimeMillis());
            }
            v.attempts++;
            v.lastAttemptTime = System.currentTimeMillis();
            return v;
        });

        int remaining = maxRetry - record.attempts;
        log.info("Login failure recorded: username={}, attempts={}/{}, remaining={}",
                username, record.attempts, maxRetry, Math.max(0, remaining));
        return Math.max(0, remaining);
    }

    @Override
    public void clearLoginFailure(String username) {
        failMap.remove(username);
    }

    @Override
    public boolean isAccountLocked(String username) {
        LoginFailRecord record = failMap.get(username);
        if (record == null) {
            return false;
        }
        int maxRetry = getLoginMaxRetry();
        if (record.attempts < maxRetry) {
            return false;
        }
        // Check whether the lockout period has elapsed
        long lockDurationMs = LOCKOUT_DURATION_MINUTES * 60L * 1000L;
        if (System.currentTimeMillis() - record.lastAttemptTime > lockDurationMs) {
            failMap.remove(username);
            return false;
        }
        return true;
    }

    // ==================== Scheduled cleanup ====================

    /**
     * Clean up expired login failure records every hour
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanExpiredFailRecords() {
        long expireTime = System.currentTimeMillis() - LOCKOUT_DURATION_MINUTES * 60L * 1000L;
        failMap.entrySet().removeIf(entry -> entry.getValue().lastAttemptTime < expireTime);
    }

    // ==================== Internal classes ====================

    private static class LoginFailRecord {
        int attempts;
        long lastAttemptTime;

        LoginFailRecord(int attempts, long lastAttemptTime) {
            this.attempts = attempts;
            this.lastAttemptTime = lastAttemptTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastAttemptTime > LOCKOUT_DURATION_MINUTES * 60L * 1000L;
        }
    }
}
