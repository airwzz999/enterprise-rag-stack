package com.knowledge.base.userauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Team member VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Team member information")
public class TeamMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Real name")
    private String realName;

    @Schema(description = "User avatar")
    private String avatar;

    @Schema(description = "Member role: leader, member")
    private String role;

    @Schema(description = "Joined at")
    private LocalDateTime joinedAt;
}
