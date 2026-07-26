package com.knowledge.base.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Comprehensive dashboard data VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comprehensive dashboard data")
public class DashboardVO {

    @Schema(description = "Data overview")
    private OverviewVO overview;

    @Schema(description = "Document trend (last 7 days)")
    private List<TrendVO> documentTrend;

    @Schema(description = "Category distribution")
    private List<CategoryDistributionVO> categoryDistribution;

    @Schema(description = "Popular documents")
    private List<HotDocumentVO> hotDocuments;

    @Schema(description = "Active users")
    private List<ActiveUserVO> activeUsers;
}
