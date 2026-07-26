package com.knowledge.base.userauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Token validation response VO
 *
 * <p>Used by other microservices (e.g. kb-foundation) calling /auth/validate via Feign;
 * returns the verified user identity and role information.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token validation response")
public class TokenValidateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "User nickname")
    private String nickname;

    @Schema(description = "Avatar URL")
    private String avatar;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "User status (0-disabled, 1-enabled)")
    private Integer status;

    @Schema(description = "Role code list, e.g. [\"USER\", \"REVIEWER\"]")
    private List<String> roles;

    @Schema(description = "Whether the token is valid")
    private Boolean valid;
}
