package com.knowledge.base.search.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Search result VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search result")
public class SearchResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long id;

    /**
     * Document title (highlighted)
     */
    @Schema(description = "Document title")
    private String title;

    /**
     * Document summary (highlighted)
     */
    @Schema(description = "Document summary")
    private String summary;

    /**
     * Highlighted snippets
     */
    @Schema(description = "Highlighted snippets")
    private List<String> highlights;

    /**
     * Category name
     */
    @Schema(description = "Category name")
    private String categoryName;

    /**
     * Tag name list
     */
    @Schema(description = "Tag name list")
    private List<String> tagNames;

    /**
     * Creator name
     */
    @Schema(description = "Creator name")
    private String creatorName;

    /**
     * Team name
     */
    @Schema(description = "Team name")
    private String teamName;

    /**
     * View count
     */
    @Schema(description = "View count")
    private Integer viewCount;

    /**
     * Like count
     */
    @Schema(description = "Like count")
    private Integer likeCount;

    /**
     * Comment count
     */
    @Schema(description = "Comment count")
    private Integer commentCount;

    /**
     * Publish time
     */
    @Schema(description = "Publish time")
    private String publishAt;

    /**
     * Relevance score
     */
    @Schema(description = "Relevance score")
    private Float score;

    /**
     * BM25 score (hybrid search mode)
     */
    @Schema(description = "BM25 score")
    private Double bm25Score;

    /**
     * Vector similarity score (hybrid search mode)
     */
    @Schema(description = "Vector similarity score")
    private Double vectorScore;

    /**
     * LLM re-ranking score (hybrid search mode)
     */
    @Schema(description = "LLM re-ranking score")
    private Double rerankScore;

    /**
     * Document chunk result list (hybrid search mode)
     */
    @Schema(description = "Document chunk result list")
    private List<ChunkResult> chunks;

    /**
     * Document chunk result
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Document chunk retrieval result")
    public static class ChunkResult implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "Chunk ID")
        private String chunkId;

        @Schema(description = "Chunk text content")
        private String content;

        @Schema(description = "Enclosing section heading")
        private String heading;

        @Schema(description = "Fused/re-ranking score")
        private double score;

        @Schema(description = "BM25 score")
        private double bm25Score;

        @Schema(description = "Vector similarity score")
        private double vectorScore;
    }
}
