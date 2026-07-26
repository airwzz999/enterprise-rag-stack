package com.knowledge.base.userauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Team VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Team information")
public class TeamVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Team ID
     */
    @Schema(description = "Team ID")
    private Long id;

    /**
     * Team name
     */
    @Schema(description = "Team name")
    private String teamName;

    /**
     * Team code
     */
    @Schema(description = "Team code")
    private String teamCode;

    /**
     * Team description
     */
    @Schema(description = "Team description")
    private String description;

    /**
     * Team icon
     */
    @Schema(description = "Team icon")
    private String icon;

    /**
     * Parent team ID
     */
    @Schema(description = "Parent team ID")
    private Long parentId;

    /**
     * Parent team name
     */
    @Schema(description = "Parent team name")
    private String parentName;

    /**
     * Team level
     */
    @Schema(description = "Team level")
    private Integer level;

    /**
     * Member count
     */
    @Schema(description = "Member count")
    private Integer memberCount;

    /**
     * Document count
     */
    @Schema(description = "Document count")
    private Integer docCount;

    /**
     * Team leader ID
     */
    @Schema(description = "Team leader ID")
    private Long leaderId;

    /**
     * Team leader name
     */
    @Schema(description = "Team leader name")
    private String leaderName;

    /**
     * Status
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Created at
     */
    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    /**
     * Child team list
     */
    @Schema(description = "Child team list")
    private List<TeamVO> children;
}
