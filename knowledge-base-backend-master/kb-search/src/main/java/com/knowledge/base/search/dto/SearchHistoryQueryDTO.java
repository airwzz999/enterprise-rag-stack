package com.knowledge.base.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Search history query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search history query request")
public class SearchHistoryQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
    @Schema(description = "User ID")
    private Long userId;

    /**
     * Keyword (fuzzy query)
     */
    @Schema(description = "Keyword")
    private String keyword;

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
     * Page number
     */
    @Schema(description = "Page number")
    private Integer current = 1;

    /**
     * Page size
     */
    @Schema(description = "Page size")
    private Integer size = 20;

    /**
     * Sort field
     */
    @Schema(description = "Sort field")
    private String sortBy = "createdAt";

    /**
     * Sort direction
     */
    @Schema(description = "Sort direction")
    private String sortOrder = "desc";
}
