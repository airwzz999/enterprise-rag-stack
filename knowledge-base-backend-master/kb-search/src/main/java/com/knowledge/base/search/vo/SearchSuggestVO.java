package com.knowledge.base.search.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Search suggestion VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search suggestion")
public class SearchSuggestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Suggestion text
     */
    @Schema(description = "Suggestion text")
    private String text;

    /**
     * Suggestion type
     */
    @Schema(description = "Suggestion type")
    private String type;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Match score
     */
    @Schema(description = "Match score")
    private Float score;
}
