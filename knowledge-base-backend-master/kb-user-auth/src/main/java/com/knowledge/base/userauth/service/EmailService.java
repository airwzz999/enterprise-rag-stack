package com.knowledge.base.userauth.service;

/**
 * Email sending service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface EmailService {

    /**
     * Send an account activation email
     *
     * @param to       recipient email
     * @param username username
     * @param token    activation token
     */
    void sendActivationEmail(String to, String username, String token);

    /**
     * Send a password reset verification code email
     *
     * @param to   recipient email
     * @param code 6-digit verification code
     */
    void sendResetCodeEmail(String to, String code);
}
