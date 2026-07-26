package com.knowledge.base.userauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * User login response VO
 *
 * <p>Designed following the Alibaba Java Development Guidelines; used to return information after a successful login</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User login response information")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Access token
     */
    @Schema(description = "Access token")
    private String accessToken;

    /**
     * Refresh token
     */
    @Schema(description = "Refresh token")
    private String refreshToken;

    /**
     * Token type
     */
    @Schema(description = "Token type")
    private String tokenType;

    /**
     * Expires in (seconds)
     */
    @Schema(description = "Expires in (seconds)")
    private Long expiresIn;

    /**
     * User information
     */
    @Schema(description = "User information")
    private UserInfo userInfo;

    /**
     * User information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User information")
    public static class UserInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * User ID
         */
        @Schema(description = "User ID")
        private Long userId;

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
         * Gender
         */
        @Schema(description = "Gender")
        private Integer gender;

        /**
         * Role list
         */
        @Schema(description = "Role list")
        private List<String> roles;

        /**
         * Permission list
         */
        @Schema(description = "Permission list")
        private List<String> permissions;
    }
}
