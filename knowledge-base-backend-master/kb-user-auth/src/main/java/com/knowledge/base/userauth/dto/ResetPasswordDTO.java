package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Reset password request
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Reset password request")
public class ResetPasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Registered email", required = true)
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Verification code", required = true)
    @NotBlank(message = "Verification code must not be blank")
    private String code;

    @Schema(description = "New password", required = true)
    @NotBlank(message = "New password must not be blank")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String newPassword;
}
