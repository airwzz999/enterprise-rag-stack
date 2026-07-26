package com.knowledge.base.ai.rag.service;

import com.knowledge.base.ai.vo.ReindexProgressVO;

import java.util.List;

/**
 * Reindex service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface ReindexService {

    /**
     * Reindex all published documents
     */
    String reindexAll();

    /**
     * Reindex the specified document
     */
    String reindexByDocId(Long documentId);

    /**
     * Batch-reindex documents
     */
    String reindexBatch(List<Long> documentIds);

    /**
     * Get reindex progress
     */
    ReindexProgressVO getProgress(String taskId);

    /**
     * Delete the vector index for the specified document
     */
    String deleteByDocId(Long documentId);

    /**
     * Batch-delete the vector index for the specified documents
     */
    String deleteByDocIds(List<Long> documentIds);
}
