package com.knowledge.base.userauth.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Team query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Team query request")
public class TeamQueryDTO extends PageParam {

    private static final long serialVersionUID = 1L;

    /**
     * Team name (fuzzy match)
     */
    @Schema(description = "Team name")
    private String teamName;

    /**
     * Team code
     */
    @Schema(description = "Team code")
    private String teamCode;

    /**
     * Parent team ID
     */
    @Schema(description = "Parent team ID")
    private Long parentId;

    /**
     * Status
     */
    @Schema(description = "Status")
    private Integer status;
}
