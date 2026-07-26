package com.knowledge.base.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * File category request DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "File category request")
public class CategoryDTO {

    /**
     * Category ID (used when updating)
     */
    @Schema(description = "Category ID")
    private Long id;

    /**
     * Category name
     */
    @NotBlank(message = "Category name must not be blank")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    @Schema(description = "Category name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * Parent category ID (0 indicates a top-level category)
     */
    @NotNull(message = "Parent category ID must not be null")
    @Schema(description = "Parent category ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long parentId;

    /**
     * Sort order
     */
    @Schema(description = "Sort order")
    private Integer sortOrder;

    /**
     * Category icon
     */
    @Size(max = 255, message = "Icon path must not exceed 255 characters")
    @Schema(description = "Category icon")
    private String icon;

    /**
     * Category description
     */
    @Size(max = 500, message = "Category description must not exceed 500 characters")
    @Schema(description = "Category description")
    private String description;

    /**
     * Status: 0-disabled, 1-enabled
     */
    @Schema(description = "Status")
    private Integer status;
}
