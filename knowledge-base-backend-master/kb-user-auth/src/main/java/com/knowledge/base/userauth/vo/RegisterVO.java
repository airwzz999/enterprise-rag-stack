package com.knowledge.base.userauth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Registration response VO
 *
 * <p>If the user provided an email during registration, email verification is required
 * to activate the account; if no email was provided, registration succeeds immediately
 * and the user is logged in automatically.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVO implements Serializable {

    /**
     * Newly registered user ID
     */
    private Long userId;

    /**
     * Whether email verification is required
     */
    private boolean emailVerificationRequired;

    /**
     * Prompt message
     */
    private String message;

    /**
     * Login info (only set when email verification is not required)
     */
    private LoginVO loginInfo;
}
