package com.knowledge.base.ai.rag.kag.retrieval;

import com.knowledge.base.ai.vo.RagSearchResultVO;

import java.util.List;

/**
 * Hybrid retrieval service interface
 *
 * <p>Combines RAG (text vector retrieval) and KAG (knowledge graph reasoning)
 * retrieval modes to provide more comprehensive knowledge retrieval.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface HybridRetrievalService {

    /**
     * Hybrid retrieval result
     */
    record HybridResult(
            /** RAG text retrieval results */
            List<RagSearchResultVO> ragResults,
            /** KAG graph retrieval context */
            GraphContext kagContext,
            /** Final fused results */
            List<RagSearchResultVO> fusedResults,
            /** Whether knowledge graph augmentation was applied */
            boolean knowledgeGraphEnhanced
    ) {}

    /**
     * Perform hybrid retrieval (RAG + KAG in parallel → merge and deduplicate → rerank)
     *
     * @param query         the user query
     * @param topK          the number of final results to return
     * @param enableRerank  whether LLM reranking is enabled
     * @param enableKAG     whether knowledge graph augmentation is enabled
     * @param userId        the requesting user's ID, used to scope RAG results to documents
     *                      they can see (public or authored by them); null means public-only
     * @return the hybrid retrieval result
     */
    HybridResult retrieveHybrid(String query, int topK, boolean enableRerank, boolean enableKAG, Long userId);
}
