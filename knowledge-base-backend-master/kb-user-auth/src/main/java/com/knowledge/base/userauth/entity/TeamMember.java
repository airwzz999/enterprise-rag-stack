package com.knowledge.base.userauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Team member entity
 *
 * <p>Maps to the kb_team_member table, recording the membership relationship between teams and users</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("kb_team_member")
@Schema(description = "Team member entity")
public class TeamMember implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Primary key ID")
    private Long id;

    /**
     * Team ID
     */
    @Schema(description = "Team ID")
    private Long teamId;

    /**
     * User ID
     */
    @Schema(description = "User ID")
    private Long userId;

    /**
     * Member role: leader, member
     */
    @Schema(description = "Member role: leader, member")
    private String memberRole;

    /**
     * Joined at
     */
    @Schema(description = "Joined at")
    private LocalDateTime joinTime;

    /**
     * Added by (user ID)
     */
    @Schema(description = "Added by (user ID)")
    private Long createBy;
}
