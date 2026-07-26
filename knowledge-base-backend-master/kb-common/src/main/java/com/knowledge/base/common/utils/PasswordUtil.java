package com.knowledge.base.common.utils;

import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * Password utility class
 *
 * <p>Provides password encryption and verification functionality</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class PasswordUtil {

    /**
     * Encrypt a password
     *
     * @param password the plaintext password
     * @return the encrypted password
     */
    public static String encrypt(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Verify a password
     *
     * @param password       the plaintext password
     * @param hashedPassword the encrypted password
     * @return whether they match
     */
    public static boolean verify(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

    /**
     * Generate a random password
     *
     * @param length the password length
     * @return the random password
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }

    /**
     * Check password strength
     *
     * @param password the password
     * @return the strength level (0-4)
     */
    public static int checkStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int strength = 0;

        // Length check
        if (password.length() >= 8) {
            strength++;
        }
        if (password.length() >= 12) {
            strength++;
        }

        // Contains lowercase letters
        if (password.matches(".*[a-z].*")) {
            strength++;
        }

        // Contains uppercase letters
        if (password.matches(".*[A-Z].*")) {
            strength++;
        }

        // Contains digits
        if (password.matches(".*\\d.*")) {
            strength++;
        }

        // Contains special characters
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            strength++;
        }

        return Math.min(strength, 4);
    }

    /**
     * Get the password strength description
     *
     * @param strength the strength level
     * @return the description
     */
    public static String getStrengthDescription(int strength) {
        return switch (strength) {
            case 0 -> "Very weak";
            case 1 -> "Weak";
            case 2 -> "Fair";
            case 3 -> "Strong";
            case 4 -> "Very strong";
            default -> "Unknown";
        };
    }
}
