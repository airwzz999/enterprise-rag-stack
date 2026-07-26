package com.knowledge.base.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Statistics query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Statistics query parameters")
public class StatisticsQueryDTO implements Serializable {

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
     * User ID
     */
    @Schema(description = "User ID")
    private Long userId;

    /**
     * Category ID
     */
    @Schema(description = "Category ID")
    private Long categoryId;

    /**
     * Team ID
     */
    @Schema(description = "Team ID")
    private Long teamId;

    /**
     * Document status
     */
    @Schema(description = "Document status")
    private Integer documentStatus;

    /**
     * Whether to include subcategories
     */
    @Schema(description = "Whether to include subcategories")
    private Boolean includeChildren;

    /**
     * Sort field
     */
    @Schema(description = "Sort field")
    private String sortBy;

    /**
     * Sort direction
     */
    @Schema(description = "Sort direction")
    private String sortOrder;

    /**
     * Result count limit
     */
    @Schema(description = "Result count limit")
    private Integer limit;
}
