package com.knowledge.base.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data overview VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data overview information")
public class OverviewVO {

    @Schema(description = "Total documents")
    private Long totalDocuments;

    @Schema(description = "Total users")
    private Long totalUsers;

    @Schema(description = "New documents today")
    private Long todayDocuments;

    @Schema(description = "New users today")
    private Long todayUsers;

    @Schema(description = "Total view count")
    private Long totalViews;

    @Schema(description = "Views today")
    private Long todayViews;

    @Schema(description = "Total likes")
    private Long totalLikes;

    @Schema(description = "Total favorites")
    private Long totalFavorites;

    @Schema(description = "Total comments")
    private Long totalComments;

    @Schema(description = "Documents pending review")
    private Long pendingReviews;

    @Schema(description = "AI smart search count")
    private Long aiSearchCount;

    @Schema(description = "AI Q&A count")
    private Long aiQaCount;

    @Schema(description = "Active users (last 30 days)")
    private Long activeUserCount;
}
