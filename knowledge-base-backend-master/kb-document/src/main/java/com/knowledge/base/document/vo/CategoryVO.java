package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Category VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Category information")
public class CategoryVO {

    @Schema(description = "Category ID")
    private Long id;

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

    @Schema(description = "Document count")
    private Long documentCount;

    @Schema(description = "Subcategory list")
    private List<CategoryVO> children;
}
