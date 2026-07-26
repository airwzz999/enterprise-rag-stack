package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * User update DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "User update request")
public class UserUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
    @Schema(description = "User ID")
    private Long id;

    /**
     * Real name
     */
    @Schema(description = "Real name")
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
    private String phone;

    /**
     * Avatar URL
     */
    @Schema(description = "Avatar URL")
    private String avatarUrl;

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
     * Status
     */
    @Schema(description = "Status")
    private Integer status;
}
