package com.knowledge.base.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.security.Keys;

/**
 * JWT configuration class
 *
 * <p>Provides JWT token configuration properties, including the secret key, expiration time, and issuer information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT secret key
     */
    private String secret = "please-change-this-to-a-long-random-secret-in-production";

    /**
     * Access token expiration time (seconds)
     */
    private Long expiration = 7200L;

    /**
     * Refresh token expiration time (seconds)
     */
    private Long refreshExpiration = 604800L;

    /**
     * Issuer
     */
    private String issuer = "knowledge-base";

    /**
     * Get the secret key object
     *
     * @return the secret key object
     */
    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
