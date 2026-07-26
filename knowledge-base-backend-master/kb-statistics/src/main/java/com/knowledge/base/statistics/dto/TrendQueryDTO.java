package com.knowledge.base.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Trend query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Trend query parameters")
public class TrendQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Start time
     */
    @Schema(description = "Start time")
    private LocalDateTime startTime;

    /**
     * End time
     */
    @Schema(description = "End time")
    private LocalDateTime endTime;

    /**
     * Time interval (day, week, month)
     */
    @Schema(description = "Time interval")
    private String interval;

    /**
     * Trend type (document, user, view, like, comment)
     */
    @Schema(description = "Trend type")
    private String trendType;

    /**
     * Category ID
     */
    @Schema(description = "Category ID")
    private Long categoryId;

    /**
     * User ID
     */
    @Schema(description = "User ID")
    private Long userId;

    /**
     * Team ID
     */
    @Schema(description = "Team ID")
    private Long teamId;

    /**
     * Whether to include subcategories
     */
    @Schema(description = "Whether to include subcategories")
    private Boolean includeChildren;

    /**
     * Number of data points
     */
    @Schema(description = "Number of data points")
    private Integer dataPoints;
}
