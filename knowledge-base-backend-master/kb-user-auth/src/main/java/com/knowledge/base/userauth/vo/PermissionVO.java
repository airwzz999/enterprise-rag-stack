package com.knowledge.base.userauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Permission VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Permission information")
public class PermissionVO {

    @Schema(description = "Permission ID")
    private Long id;

    @Schema(description = "Permission name")
    private String name;

    @Schema(description = "Permission code")
    private String code;

    @Schema(description = "Permission type")
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

    @Schema(description = "Status")
    private Integer status;

    @Schema(description = "Child permission list")
    private List<PermissionVO> children;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at")
    private LocalDateTime updatedAt;
}
