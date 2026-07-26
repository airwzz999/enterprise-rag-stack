package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Verify password reset code request
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Verify password reset code request")
public class VerifyResetCodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Registered email", required = true)
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "6-digit verification code", required = true)
    @NotBlank(message = "Verification code must not be blank")
    @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
    private String code;
}
