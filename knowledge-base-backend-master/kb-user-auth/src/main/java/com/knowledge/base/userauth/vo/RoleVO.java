package com.knowledge.base.userauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Role VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role information")
public class RoleVO {

    @Schema(description = "Role ID")
    private Long id;

    @Schema(description = "Role name")
    private String name;

    @Schema(description = "Role code")
    private String code;

    @Schema(description = "Role description")
    private String description;

    @Schema(description = "Status")
    private Integer status;

    @Schema(description = "Permission list")
    private List<String> permissions;

    @Schema(description = "Number of bound users")
    private Long userCount;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at")
    private LocalDateTime updatedAt;
}
