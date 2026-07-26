package com.knowledge.base.userauth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Team entity
 *
 * <p>Extends BaseEntity, reusing common fields such as ID, created/updated timestamps, created/updated by, and soft delete</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_team")
@Schema(description = "Team entity")
public class Team extends BaseEntity {

    private static final long serialVersionUID = 1L;

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
     * Team level
     */
    @Schema(description = "Team level")
    private Integer level;

    /**
     * Team path
     */
    @Schema(description = "Team path")
    private String path;

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
     * Status: 0-disabled, 1-active
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Sort order
     */
    @Schema(description = "Sort order")
    private Integer sort;
}
