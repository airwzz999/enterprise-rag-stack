package com.knowledge.base.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Popular document VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Popular documents")
public class HotDocumentVO {

    @Schema(description = "Document ID")
    private Long documentId;

    @Schema(description = "Document title")
    private String title;

    @Schema(description = "Author ID")
    private Long authorId;

    @Schema(description = "Author name")
    private String authorName;

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "View count")
    private Long viewCount;

    @Schema(description = "Like count")
    private Long likeCount;

    @Schema(description = "Favorite count")
    private Long favoriteCount;

    @Schema(description = "Comment count")
    private Long commentCount;

    @Schema(description = "Document summary")
    private String summary;

    @Schema(description = "Creation time")
    private String createdAt;

    @Schema(description = "Statistic value")
    private Long statisticsValue;
}
