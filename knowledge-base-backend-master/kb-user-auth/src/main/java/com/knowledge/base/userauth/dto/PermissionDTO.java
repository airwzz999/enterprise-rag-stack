package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Permission DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Permission information")
public class PermissionDTO {

    @Schema(description = "Permission ID")
    private Long id;

    @NotBlank(message = "Permission name must not be blank")
    @Schema(description = "Permission name")
    private String name;

    @NotBlank(message = "Permission code must not be blank")
    @Schema(description = "Permission code")
    private String code;

    @Schema(description = "Permission type (menu, button, api)")
    private String type;

    @Schema(description = "Parent permission ID")
    private Long parentId;

    @Schema(description = "Menu URL")
    private String menuUrl;

    @Schema(description = "API URL")
    private String apiUrl;

    @Schema(description = "Request method")
    private String method;

    @Schema(description = "Permission description")
    private String description;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Status (0-disabled, 1-enabled)")
    private Integer status;
}
