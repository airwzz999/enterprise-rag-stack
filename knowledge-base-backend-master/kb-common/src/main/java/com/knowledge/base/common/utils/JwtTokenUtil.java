package com.knowledge.base.common.utils;

import com.knowledge.base.common.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT utility class
 *
 * <p>Provides functionality to generate, parse, and validate JWT tokens</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtTokenUtil {

    @Resource
    private JwtConfig jwtConfig;

    /**
     * Generate an access token
     *
     * @param userId the user ID
     * @return the token
     */
    public String generateAccessToken(Long userId) {
        return generateToken(userId, null, null, jwtConfig.getExpiration() * 1000);
    }

    /**
     * Generate an access token (with user info)
     *
     * @param userId the user ID
     * @param username the username
     * @param avatar the avatar URL
     * @return the token
     */
    public String generateAccessToken(Long userId, String username, String avatar) {
        return generateToken(userId, username, avatar, jwtConfig.getExpiration() * 1000);
    }

    /**
     * Generate an access token (with a specified expiration time)
     *
     * @param userId the user ID
     * @param username the username
     * @param avatar the avatar URL
     * @param expirationSeconds the expiration time (seconds)
     * @return the token
     */
    public String generateAccessToken(Long userId, String username, String avatar, Long expirationSeconds) {
        return generateToken(userId, username, avatar, expirationSeconds * 1000);
    }

    /**
     * Generate a refresh token
     *
     * @param userId the user ID
     * @return the token
     */
    public String generateRefreshToken(Long userId) {
        return generateToken(userId, null, null, jwtConfig.getRefreshExpiration() * 1000);
    }

    /**
     * Generate a token
     *
     * @param userId the user ID
     * @param expiration the expiration time (milliseconds)
     * @return the token
     */
    private String generateToken(Long userId, String username, String avatar, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "access");
        if (username != null) {
            claims.put("username", username);
        }
        if (avatar != null) {
            claims.put("avatar", avatar);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtConfig.secretKey())
                .compact();
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
                    .verifyWith(jwtConfig.secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the user ID from a token
     *
     * @param token the token
     * @return the user ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Get the username from a token
     *
     * @param token the token
     * @return the username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object username = claims.get("username");
        return username != null ? username.toString() : null;
    }

    /**
     * Get the avatar URL from a token
     *
     * @param token the token
     * @return the avatar URL
     */
    public String getAvatarFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object avatar = claims.get("avatar");
        return avatar != null ? avatar.toString() : null;
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
            if (claims == null) {
                return false;
            }
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            log.error("Failed to validate token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check whether a token is about to expire
     *
     * @param token the token
     * @param thresholdSeconds the threshold (seconds)
     * @return whether it is about to expire
     */
    public boolean isTokenExpiringSoon(String token, int thresholdSeconds) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                return true;
            }
            Date expiration = claims.getExpiration();
            long timeToExpiry = expiration.getTime() - System.currentTimeMillis();
            return timeToExpiry < thresholdSeconds * 1000;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Refresh a token
     *
     * @param refreshToken the refresh token
     * @return the new access token
     */
    public String refreshToken(String refreshToken) {
        Claims claims = parseToken(refreshToken);
        if (claims == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        Long userId = Long.parseLong(claims.getSubject());
        return generateAccessToken(userId);
    }
}
