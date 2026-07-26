package com.knowledge.base.userauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Role information VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role information")
public class RoleInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Role ID
     */
    @Schema(description = "Role ID")
    private Long id;

    /**
     * Role code
     */
    @Schema(description = "Role code")
    private String roleCode;

    /**
     * Role name
     */
    @Schema(description = "Role name")
    private String roleName;

    /**
     * Role description
     */
    @Schema(description = "Role description")
    private String description;
}
