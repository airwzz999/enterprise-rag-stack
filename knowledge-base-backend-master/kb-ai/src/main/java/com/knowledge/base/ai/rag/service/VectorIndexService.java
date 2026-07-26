package com.knowledge.base.ai.rag.service;

import com.knowledge.base.ai.rag.entity.DocumentChunk;
import com.knowledge.base.ai.vo.RagSearchResultVO;

import java.util.List;

/**
 * Vector index service interface
 *
 * <p>Manages the ES index for document chunks (write, delete, hybrid search),
 * supporting BM25 keyword search + kNN vector search + RRF fusion.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface VectorIndexService {

    /**
     * Batch-index document chunks
     *
     * @param chunks list of document chunks
     */
    void indexChunks(List<DocumentChunk> chunks);

    /**
     * Delete all indexed chunks by document ID
     *
     * @param documentId document ID
     */
    void deleteByDocId(Long documentId);

    /**
     * Hybrid search (BM25 + kNN + RRF fusion)
     *
     * @param queryText          query text
     * @param queryEmbedding     query vector
     * @param topK               top K results to return
     * @param hybridTopK         number of candidates returned by each of BM25 and kNN
     * @param rrfC               RRF constant
     * @return the fused and ranked search results
     */
    List<RagSearchResultVO> searchHybrid(String queryText, float[] queryEmbedding,
                                          int topK, int hybridTopK, int rrfC);

    /**
     * Check whether the kb_chunk index exists
     */
    boolean indexExists();

    /**
     * Create the kb_chunk index (if it doesn't exist)
     */
    void createIndexIfNotExists();

    /**
     * Delete the entire kb_chunk index
     */
    void dropIndex();
}
