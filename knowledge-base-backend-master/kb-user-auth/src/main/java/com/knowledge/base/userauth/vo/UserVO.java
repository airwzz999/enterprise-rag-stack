package com.knowledge.base.userauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User response VO
 *
 * <p>Designed following the Alibaba Java Development Guidelines; used to return user information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "User information response")
public class UserVO implements Serializable {

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
     * Nickname
     */
    @Schema(description = "Nickname")
    private String nickname;

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
    private String avatar;

    /**
     * Gender (0-unknown, 1-male, 2-female)
     */
    @Schema(description = "Gender")
    private Integer gender;

    /**
     * Status (0-disabled, 1-enabled)
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Remark
     */
    @Schema(description = "Remark")
    private String remark;

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
     * Primary role code
     */
    @Schema(description = "Primary role code")
    private String role;

    /**
     * Role code list
     */
    @Schema(description = "Role code list")
    private List<String> roles;

    /**
     * Permission code list
     */
    @Schema(description = "Permission code list")
    private List<String> permissions;

    /**
     * Department ID
     */
    @Schema(description = "Department ID")
    private Long deptId;

    /**
     * Post ID
     */
    @Schema(description = "Post ID")
    private Long postId;

    /**
     * Created at
     */
    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    /**
     * Updated at
     */
    @Schema(description = "Updated at")
    private LocalDateTime updatedAt;

    /**
     * Last login time
     */
    @Schema(description = "Last login time")
    private LocalDateTime lastLoginTime;

    /**
     * Last login IP
     */
    @Schema(description = "Last login IP")
    private String lastLoginIp;
}
