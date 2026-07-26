package com.knowledge.base.userauth.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Verification token utility class
 *
 * <p>Used to generate and validate account activation tokens</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Component
public class VerificationTokenUtil {

    @Value("${app.activation-token-expiry-hours:24}")
    private int tokenExpiryHours;

    /**
     * Generate an activation token
     *
     * @return UUID string
     */
    public String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Calculate the token expiry time
     *
     * @return expiry time
     */
    public LocalDateTime calculateExpiryTime() {
        return LocalDateTime.now().plusHours(tokenExpiryHours);
    }

    /**
     * Check whether a token has expired
     *
     * @param expiryTime expiry time
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(LocalDateTime expiryTime) {
        return expiryTime == null || LocalDateTime.now().isAfter(expiryTime);
    }
}
