package com.knowledge.base.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Citation source VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Citation source")
public class CitationVO {

    @Schema(description = "Citation number (corresponds to [1] [2] in the answer)")
    private int index;

    @Schema(description = "Source document ID")
    private Long documentId;

    @Schema(description = "Source document title")
    private String documentTitle;

    @Schema(description = "Excerpt summary of the citation")
    private String excerpt;

    @Schema(description = "Relevance score")
    private double relevanceScore;
}
