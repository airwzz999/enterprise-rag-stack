package com.knowledge.base.userauth.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * Password generation utility
 * Used to generate a BCrypt hash for a password
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        // Generate the BCrypt hash for the password "123456"
        String password = "123456";
        String hashed = BCrypt.hashpw(password);

        System.out.println("========================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt hash: " + hashed);
        System.out.println("========================================");
        System.out.println();
        System.out.println("SQL update statement:");
        System.out.println("UPDATE kb_user.user SET password = '" + hashed + "' WHERE username = 'admin';");
        System.out.println();
        System.out.println("Verify the password matches:");
        System.out.println("Verification result: " + BCrypt.checkpw(password, hashed));
    }
}
