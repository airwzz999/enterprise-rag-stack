package com.knowledge.base.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Search request DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Search request")
public class SearchRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Search keyword
     */
    @Schema(description = "Search keyword")
    @NotBlank(message = "Search keyword must not be blank")
    private String keyword;

    /**
     * Category ID list
     */
    @Schema(description = "Category ID list")
    private List<Long> categoryIds;

    /**
     * Tag ID list
     */
    @Schema(description = "Tag ID list")
    private List<Long> tagIds;

    /**
     * Team ID list
     */
    @Schema(description = "Team ID list")
    private List<Long> teamIds;

    /**
     * Creator ID
     */
    @Schema(description = "Creator ID")
    private Long creatorId;

    /**
     * Document status
     */
    @Schema(description = "Document status")
    private Integer docStatus;

    /**
     * Start time
     */
    @Schema(description = "Start time")
    private String startTime;

    /**
     * End time
     */
    @Schema(description = "End time")
    private String endTime;

    /**
     * Sort field
     */
    @Schema(description = "Sort field")
    private String sortField;

    /**
     * Sort order
     */
    @Schema(description = "Sort order")
    private String sortOrder;

    /**
     * Page number
     */
    @Schema(description = "Page number")
    private Integer current = 1;

    /**
     * Page size
     */
    @Schema(description = "Page size")
    private Integer size = 10;

    /**
     * Search mode: keyword (keyword search) / hybrid (hybrid intelligent search)
     */
    @Schema(description = "Search mode: keyword / hybrid", example = "hybrid")
    private String searchMode = "keyword";

    /**
     * Number of Top-K results to return for hybrid search (effective when searchMode=hybrid)
     */
    @Schema(description = "Hybrid search Top-K")
    private int topK = 10;

    /**
     * Whether to enable LLM re-ranking (effective when searchMode=hybrid)
     */
    @Schema(description = "Whether to enable LLM re-ranking")
    private boolean enableRerank = true;
}
