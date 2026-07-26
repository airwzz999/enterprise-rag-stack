package com.knowledge.base.userauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * User information VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information")
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
    @Schema(description = "User ID")
    private Long id;

    /**
     * Username
     */
    @Schema(description = "Username")
    private String username;

    /**
     * Real name
     */
    @Schema(description = "Real name")
    private String realName;

    /**
     * Email
     */
    @Schema(description = "Email")
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
    private String department;

    /**
     * Position
     */
    @Schema(description = "Position")
    private String position;

    /**
     * Bio
     */
    @Schema(description = "Bio")
    private String bio;

    /**
     * User type
     */
    @Schema(description = "User type")
    private Integer userType;

    /**
     * Role list
     */
    @Schema(description = "Role list")
    private List<RoleInfo> roles;

    /**
     * Permission list
     */
    @Schema(description = "Permission list")
    private List<String> permissions;
}
