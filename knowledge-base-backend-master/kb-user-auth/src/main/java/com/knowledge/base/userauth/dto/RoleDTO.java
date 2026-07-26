package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Role DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Role information")
public class RoleDTO {

    @Schema(description = "Role ID")
    private Long id;

    @NotBlank(message = "Role name must not be blank")
    @Schema(description = "Role name")
    private String name;

    @Schema(description = "Role code")
    private String code;

    @Schema(description = "Role description")
    private String description;

    @Schema(description = "Status (0-disabled, 1-enabled)")
    private Integer status;
}
