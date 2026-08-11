package com.knowledge.base.ai.rag.service;

import com.knowledge.base.ai.vo.RagSearchResultVO;

import java.util.List;

/**
 * RAG retrieval service interface
 *
 * <p>Orchestrates the retrieval pipeline: embed query → hybrid retrieval → rerank → return results</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface RagRetrievalService {

    /**
     * RAG retrieval
     *
     * @param query        query text
     * @param topK         top K results to return
     * @param enableRerank whether LLM reranking is enabled
     * @param userId       the requesting user's ID, used to scope results to documents they
     *                     can see (public or authored by them); null means public-only
     * @return the list of retrieval results
     */
    List<RagSearchResultVO> retrieve(String query, int topK, boolean enableRerank, Long userId);
}
