package com.knowledge.base.userauth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JWT configuration class
 *
 * <p>Configures JWT token generation and validation</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration("userAuthJwtConfig")
public class JwtConfig {

    @Value("${jwt.secret:please-change-this-to-a-long-random-secret-in-production}")
    private String secret;

    @Value("${jwt.expiration:7200}")
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800}")
    private Long refreshExpiration;

    @Value("${jwt.issuer:knowledge-base}")
    private String issuer;

    /**
     * Get the secret key
     *
     * @return SecretKey
     */
    @Bean
    public SecretKey secretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Ensure the key is at least 256 bits
        if (keyBytes.length < 32) {
            // If the key is too short, expand it using Base64 encoding
            keyBytes = Base64.getEncoder().encode(secret.getBytes(StandardCharsets.UTF_8));
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * Get the token expiration time
     *
     * @return expiration time (seconds)
     */
    public Long getExpiration() {
        return expiration;
    }

    /**
     * Get the refresh token expiration time
     *
     * @return expiration time (seconds)
     */
    public Long getRefreshExpiration() {
        return refreshExpiration;
    }

    /**
     * Get the issuer
     *
     * @return issuer
     */
    public String getIssuer() {
        return issuer;
    }
}
