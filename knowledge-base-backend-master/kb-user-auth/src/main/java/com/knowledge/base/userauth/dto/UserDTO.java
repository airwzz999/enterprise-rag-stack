package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * User DTO
 *
 * <p>Designed following the Alibaba Java Development Guidelines; used to receive user creation/update request parameters</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "User information request parameters")
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
    @Schema(description = "User ID", example = "1234567890123456789")
    private Long id;

    /**
     * Username
     */
    @Schema(description = "Username", required = true, example = "zhangsan")
    @NotBlank(message = "Username must not be blank")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, digits, and underscores")
    private String username;

    /**
     * Password
     */
    @Schema(description = "Password", example = "123456")
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    private String password;

    /**
     * Nickname
     */
    @Schema(description = "Nickname", example = "Zhang San")
    @Size(max = 50, message = "Nickname must not exceed 50 characters")
    private String nickname;

    /**
     * Real name
     */
    @Schema(description = "Real name", example = "Zhang San")
    @Size(max = 50, message = "Real name must not exceed 50 characters")
    private String realName;

    /**
     * Email
     */
    @Schema(description = "Email", example = "zhangsan@example.com")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * Phone number
     */
    @Schema(description = "Phone number", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phone;

    /**
     * Avatar URL
     */
    @Schema(description = "Avatar URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    /**
     * Gender (0-unknown, 1-male, 2-female)
     */
    @Schema(description = "Gender (0-unknown, 1-male, 2-female)", example = "1")
    private Integer gender;

    /**
     * Status (0-disabled, 1-enabled)
     */
    @Schema(description = "Status (0-disabled, 1-enabled)", example = "1")
    private Integer status;

    /**
     * Remark
     */
    @Schema(description = "Remark", example = "This is a remark")
    @Size(max = 200, message = "Remark must not exceed 200 characters")
    private String remark;

    /**
     * Department
     */
    @Schema(description = "Department", example = "Engineering")
    @Size(max = 50, message = "Department must not exceed 50 characters")
    private String department;

    /**
     * Position
     */
    @Schema(description = "Position", example = "Java Engineer")
    @Size(max = 50, message = "Position must not exceed 50 characters")
    private String position;

    /**
     * Department ID
     */
    @Schema(description = "Department ID", example = "1234567890123456789")
    private Long deptId;

    /**
     * Post ID
     */
    @Schema(description = "Post ID", example = "1234567890123456789")
    private Long postId;
}
