package com.knowledge.base.search.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Search history VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search history")
public class SearchHistoryVO {

    @Schema(description = "History ID")
    private Long id;

    @Schema(description = "Search keyword")
    private String keyword;

    @Schema(description = "Search count")
    private Integer searchCount;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
