package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * User registration DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "User registration request parameters")
public class RegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Username", required = true, example = "newuser")
    @NotBlank(message = "Username must not be blank")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    private String username;

    @Schema(description = "Password", required = true, example = "MyPass123!")
    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String password;

    @Schema(description = "Confirm password", required = true)
    @NotBlank(message = "Confirm password must not be blank")
    private String confirmPassword;

    @Schema(description = "Email", required = true, example = "user@example.com")
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Real name", required = true, example = "Zhang San")
    @NotBlank(message = "Real name must not be blank")
    @Size(max = 50, message = "Real name must not exceed 50 characters")
    private String realName;

    @Schema(description = "Phone number", example = "13800138000")
    private String phone;

    @Schema(description = "Team space ID", required = true)
    @NotNull(message = "Team space must not be null")
    private Long teamId;
}
