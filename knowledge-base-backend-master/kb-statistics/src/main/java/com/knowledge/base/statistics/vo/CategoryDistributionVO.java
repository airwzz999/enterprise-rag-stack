package com.knowledge.base.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category distribution VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Category distribution")
public class CategoryDistributionVO {

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "Document count")
    private Long documentCount;

    @Schema(description = "Percentage")
    private Double percentage;
}
