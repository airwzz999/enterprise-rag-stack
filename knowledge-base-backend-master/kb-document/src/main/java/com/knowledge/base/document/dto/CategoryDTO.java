package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Category DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Category information")
public class CategoryDTO {

    @Schema(description = "Category ID")
    private Long id;

    @NotBlank(message = "Category name must not be blank")
    @Schema(description = "Category name")
    private String name;

    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Parent category ID")
    private Long parentId;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Icon")
    private String icon;
}
