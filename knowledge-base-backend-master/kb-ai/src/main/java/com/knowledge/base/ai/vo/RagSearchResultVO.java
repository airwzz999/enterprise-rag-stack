package com.knowledge.base.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG retrieval result VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG retrieval result")
public class RagSearchResultVO {

    @Schema(description = "Chunk ID")
    private String chunkId;

    @Schema(description = "Source document ID")
    private Long documentId;

    @Schema(description = "Source document title")
    private String documentTitle;

    @Schema(description = "Chunk text content")
    private String content;

    @Schema(description = "Section heading it belongs to")
    private String heading;

    @Schema(description = "Fusion/rerank score")
    private double score;

    @Schema(description = "BM25 score")
    private double bm25Score;

    @Schema(description = "Vector similarity score")
    private double vectorScore;

    @Schema(description = "Document publish time")
    private String publishTime;
}
