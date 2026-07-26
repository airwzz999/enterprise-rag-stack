package com.knowledge.base.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT utility class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT secret key
     */
    @Value("${jwt.secret:please-change-this-to-a-long-random-secret-in-production}")
    private String secret;

    /**
     * Token validity period (milliseconds)
     */
    @Value("${jwt.expiration:7200000}")
    private Long expiration;

    /**
     * Generate a token
     *
     * @param subject    the subject (typically the user ID)
     * @param claims     custom claims
     * @return the token
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * Generate a token (without custom claims)
     *
     * @param subject the subject (typically the user ID)
     * @return the token
     */
    public String generateToken(String subject) {
        return generateToken(subject, null);
    }

    /**
     * Parse a token
     *
     * @param token the token
     * @return the Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse token: {}", e.getMessage());
            throw new RuntimeException("Failed to parse token");
        }
    }

    /**
     * Get the subject from a token
     *
     * @param token the token
     * @return the subject
     */
    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Get the user ID from a token
     *
     * @param token the token
     * @return the user ID
     */
    public Long getUserId(String token) {
        String subject = getSubject(token);
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            log.error("Failed to parse user ID: {}", subject);
            return null;
        }
    }

    /**
     * Check whether a token is valid
     *
     * @param token the token
     * @return whether it is valid
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return !expiration.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check whether a token is about to expire (less than 30 minutes remaining)
     *
     * @param token the token
     * @return whether it is about to expire
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long timeLeft = expiration.getTime() - System.currentTimeMillis();
            return timeLeft < (30 * 60 * 1000);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Refresh a token
     *
     * @param token the token
     * @return the new token
     */
    public String refreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            String subject = claims.getSubject();
            return generateToken(subject, claims);
        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the secret key
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
