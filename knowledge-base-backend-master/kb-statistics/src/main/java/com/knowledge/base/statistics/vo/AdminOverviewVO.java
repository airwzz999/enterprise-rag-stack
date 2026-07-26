package com.knowledge.base.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Admin overview VO
 *
 * <p>Used for the admin dashboard, providing a comprehensive overview of key system metrics</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin overview data")
public class AdminOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Total users")
    private Long totalUsers;

    @Schema(description = "Total documents")
    private Long totalDocuments;

    @Schema(description = "Documents pending review")
    private Long pendingReviews;

    @Schema(description = "System health (0-100)")
    private Double systemHealth;

    @Schema(description = "Total roles")
    private Long totalRoles;

    @Schema(description = "Total categories")
    private Long totalCategories;

    @Schema(description = "Total teams")
    private Long totalTeams;

    @Schema(description = "Total comments")
    private Long totalComments;

    @Schema(description = "Total likes")
    private Long totalLikes;

    @Schema(description = "Total favorites")
    private Long totalFavorites;

    @Schema(description = "Total views")
    private Long totalViews;

    @Schema(description = "AI smart search count")
    private Long aiSearchCount;

    @Schema(description = "AI Q&A count")
    private Long aiQaCount;
}
