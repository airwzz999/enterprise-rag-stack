package com.knowledge.base.ai.rag.kag.retrieval.impl;

import com.knowledge.base.ai.config.ModelProvider;
import com.knowledge.base.ai.config.KAGProperties;
import com.knowledge.base.ai.rag.kag.retrieval.GraphContext;
import com.knowledge.base.ai.rag.kag.retrieval.HybridRetrievalService;
import com.knowledge.base.ai.rag.kag.retrieval.KAGRetrievalService;
import com.knowledge.base.ai.rag.service.RagRetrievalService;
import com.knowledge.base.ai.vo.RagSearchResultVO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Hybrid retrieval service implementation
 *
 * <p>Runs RAG (text vector retrieval) and KAG (knowledge graph reasoning) in
 * parallel, merges and deduplicates the two result sets, sorts them, and returns
 * the fused retrieval result.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalServiceImpl implements HybridRetrievalService {

    private final RagRetrievalService ragRetrievalService;
    private final KAGRetrievalService kagRetrievalService;
    private final KAGProperties kagProperties;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    /** {@inheritDoc} */
    @Override
    public HybridResult retrieveHybrid(String query, int topK, boolean enableRerank, boolean enableKAG, Long userId) {
        // Step 1: Always run RAG (primary)
        List<RagSearchResultVO> ragResults;
        try {
            ragResults = ragRetrievalService.retrieve(query, topK * 2, enableRerank, userId);
        } catch (Exception e) {
            log.warn("RAG retrieval failed: {}", e.getMessage());
            ragResults = Collections.emptyList();
        }

        // Step 2: Run KAG in parallel (if enabled)
        CompletableFuture<GraphContext> kagFuture = null;
        if (enableKAG) {
            kagFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return kagRetrievalService.retrieveGraphContext(query);
                } catch (Exception e) {
                    log.warn("KAG retrieval failed, falling back to RAG only: {}", e.getMessage());
                    return GraphContext.builder().hasResults(false).build();
                }
            }, asyncTaskExecutor);
        }

        GraphContext kagContext;
        if (kagFuture != null) {
            try {
                kagContext = kagFuture.get(
                        kagProperties.getRetrieval().getTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("KAG retrieval timeout or error: {}", e.getMessage());
                kagContext = GraphContext.builder().hasResults(false).build();
            }
        } else {
            kagContext = GraphContext.builder().hasResults(false).build();
        }

        // Step 3: Merge RAG text results with KAG chunk results
        List<RagSearchResultVO> fusedResults = mergeResults(ragResults, kagContext, topK);

        return new HybridResult(
                ragResults,
                kagContext,
                fusedResults,
                kagContext.isHasResults()
        );
    }

    /**
     * Merge RAG text results with text chunks associated via the KAG graph
     *
     * <p>Strategy:
     * 1. Convert KAG's GraphChunk into RagSearchResultVO
     * 2. Merge and deduplicate with RAG results (by chunkId)
     * 3. Sort by score descending
     * 4. Take the top K</p>
     */
    private List<RagSearchResultVO> mergeResults(
            List<RagSearchResultVO> ragResults, GraphContext kagContext, int topK) {

        Map<String, RagSearchResultVO> merged = new LinkedHashMap<>();

        // Add RAG results first (higher priority)
        for (RagSearchResultVO result : ragResults) {
            String key = result.getChunkId();
            if (key == null) key = "rag_" + UUID.randomUUID();
            merged.putIfAbsent(key, result);
        }

        // Add KAG associated chunks
        if (kagContext.isHasResults() && kagContext.getAssociatedChunks() != null) {
            for (GraphContext.GraphChunk chunk : kagContext.getAssociatedChunks()) {
                String key = chunk.getChunkId();
                if (key == null || merged.containsKey(key)) continue;

                // Create a RagSearchResultVO for the KAG chunk
                RagSearchResultVO kagResult = RagSearchResultVO.builder()
                        .chunkId(chunk.getChunkId())
                        .documentId(chunk.getDocId())
                        .documentTitle(chunk.getDocTitle())
                        .content(chunk.getContent())
                        .heading(chunk.getHeading())
                        .score(0.65) // KAG source baseline score
                        .bm25Score(0.0)
                        .vectorScore(0.65)
                        .build();
                merged.put(key, kagResult);
            }
        }

        // Sort by score descending and limit to topK
        return merged.values().stream()
                .sorted(Comparator.comparingDouble(RagSearchResultVO::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }
}
