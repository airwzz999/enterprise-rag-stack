package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * User creation DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "User creation request")
public class UserCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Username
     */
    @Schema(description = "Username")
    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, digits, and underscores")
    private String username;

    /**
     * Password
     */
    @Schema(description = "Password")
    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    private String password;

    /**
     * Real name
     */
    @Schema(description = "Real name")
    @NotBlank(message = "Real name must not be blank")
    @Size(max = 50, message = "Real name must not exceed 50 characters")
    private String realName;

    /**
     * Email
     */
    @Schema(description = "Email")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * Phone number
     */
    @Schema(description = "Phone number")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phone;

    /**
     * Department
     */
    @Schema(description = "Department")
    @Size(max = 100, message = "Department name must not exceed 100 characters")
    private String department;

    /**
     * Position
     */
    @Schema(description = "Position")
    @Size(max = 100, message = "Position name must not exceed 100 characters")
    private String position;

    /**
     * Bio
     */
    @Schema(description = "Bio")
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    /**
     * User type
     */
    @Schema(description = "User type")
    private Integer userType;

    /**
     * Role ID list
     */
    @Schema(description = "Role ID list")
    private Long[] roleIds;
}
