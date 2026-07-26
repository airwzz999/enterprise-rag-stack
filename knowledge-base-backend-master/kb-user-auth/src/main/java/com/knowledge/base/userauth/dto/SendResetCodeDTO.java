package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Send password reset code request
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Send password reset code request")
public class SendResetCodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Registered email", required = true)
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;
}
