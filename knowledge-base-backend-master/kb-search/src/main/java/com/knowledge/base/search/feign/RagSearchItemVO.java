package com.knowledge.base.search.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG hybrid search result item (matches kb-ai's RagSearchResultVO)
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchItemVO {

    /** Chunk ID */
    private String chunkId;

    /** Source document ID */
    private Long documentId;

    /** Source document title */
    private String documentTitle;

    /** Chunk text content */
    private String content;

    /** Enclosing section heading */
    private String heading;

    /** Fused/re-ranking score */
    private double score;

    /** BM25 score */
    private double bm25Score;

    /** Vector similarity score */
    private double vectorScore;

    /** Document publish time */
    private String publishTime;
}
