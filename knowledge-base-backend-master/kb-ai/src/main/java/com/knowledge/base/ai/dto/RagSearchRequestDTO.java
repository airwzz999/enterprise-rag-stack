package com.knowledge.base.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * RAG retrieval request DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "RAG retrieval request")
public class RagSearchRequestDTO {

    @Schema(description = "Query text")
    @NotBlank(message = "Query text must not be blank")
    private String query;

    @Schema(description = "Number of results to return")
    private int topK = 5;

    @Schema(description = "Whether reranking is enabled")
    private boolean enableRerank = true;

    @Schema(description = "List of document IDs to restrict the search to (optional)")
    private List<Long> filterDocIds;

    @Schema(description = "Category ID to restrict the search to (optional)")
    private Long categoryId;
}
