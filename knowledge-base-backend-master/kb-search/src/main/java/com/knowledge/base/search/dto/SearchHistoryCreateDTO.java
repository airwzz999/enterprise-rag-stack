package com.knowledge.base.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Search history creation DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search history creation request")
public class SearchHistoryCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Search keyword
     */
    @Schema(description = "Search keyword")
    private String keyword;

    /**
     * Search type (basic, advanced)
     */
    @Schema(description = "Search type")
    private String searchType;

    /**
     * Number of results
     */
    @Schema(description = "Number of results")
    private Integer resultCount;

    /**
     * Search parameters (JSON format)
     */
    @Schema(description = "Search parameters")
    private String searchParams;

    /**
     * User ID
     */
    @Schema(description = "User ID")
    private Long userId;
}
