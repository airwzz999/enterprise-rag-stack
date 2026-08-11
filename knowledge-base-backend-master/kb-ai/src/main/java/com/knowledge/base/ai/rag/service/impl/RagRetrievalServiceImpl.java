package com.knowledge.base.ai.rag.service.impl;

import com.knowledge.base.ai.config.ModelProvider;
import com.knowledge.base.ai.config.RagProperties;
import com.knowledge.base.ai.rag.service.EmbeddingService;
import com.knowledge.base.ai.rag.service.RagRetrievalService;
import com.knowledge.base.ai.rag.service.VectorIndexService;
import com.knowledge.base.ai.vo.RagSearchResultVO;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG retrieval service implementation
 *
 * <p>Orchestrates the complete retrieval pipeline:
 * Query Embedding → Hybrid Search (BM25 + kNN + RRF) → LLM Reranking</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalServiceImpl implements RagRetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorIndexService vectorIndexService;
    private final ModelProvider modelProvider;
    private final RagProperties ragProperties;

    private static final Pattern RERANK_SCORE_PATTERN = Pattern.compile("\\b([1-9]|10)\\b");
    private static final int PER_CHUNK_RERANK_TIMEOUT = 10;

    /** {@inheritDoc} */
    @Override
    public List<RagSearchResultVO> retrieve(String query, int topK, boolean enableRerank, Long userId) {
        // 1. Create the index (if it doesn't exist)
        vectorIndexService.createIndexIfNotExists();

        // 2. Generate the query vector (falls back to BM25-only search on failure)
        float[] queryEmbedding = null;
        try {
            queryEmbedding = embeddingService.embed(query);
        } catch (Exception e) {
            log.warn("Query embedding failed, falling back to BM25-only search: {}", e.getMessage());
        }

        // 3. Hybrid search (BM25 + kNN + RRF fusion), taking 2x topK as candidates
        int hybridTopK = ragProperties.getRetrieval().getHybridTopK();
        int rrfC = ragProperties.getRetrieval().getRrfC();
        int candidateK = Math.max(topK * 2, hybridTopK);

        List<RagSearchResultVO> candidates = vectorIndexService.searchHybrid(
                query, queryEmbedding, candidateK, hybridTopK, rrfC, userId);

        if (candidates.isEmpty()) {
            log.info("RAG retrieval returned no results: query={}", query);
            return List.of();
        }

        // 4. LLM reranking
        if (enableRerank && ragProperties.getRerank().isEnabled() && candidates.size() > topK) {
            candidates = rerank(candidates, query, topK);
        } else if (candidates.size() > topK) {
            candidates = candidates.subList(0, topK);
        }

        log.info("RAG retrieval completed: query={}, results={}", query, candidates.size());
        return candidates;
    }

    /**
     * LLM reranking
     *
     * <p>Calls the LLM to score each candidate chunk (1-10) and sorts them by score descending.</p>
     */
    private List<RagSearchResultVO> rerank(List<RagSearchResultVO> candidates, String query, int topK) {
        try {
            ChatLanguageModel model = modelProvider.getDefaultModel();

            List<ScoredChunk> scored = new ArrayList<>();
            for (RagSearchResultVO candidate : candidates) {
                String prompt = buildRerankPrompt(query, candidate.getContent());
                try {
                    String response = model.generate(UserMessage.from(prompt)).content().text();
                    int score = parseRelevanceScore(response);
                    scored.add(new ScoredChunk(candidate, score));
                } catch (Exception e) {
                    log.warn("Rerank scoring failed: chunkId={}, error={}", candidate.getChunkId(), e.getMessage());
                    // Keep the original RRF score on failure
                    scored.add(new ScoredChunk(candidate, (int) (candidate.getScore() * 10)));
                }
            }

            scored.sort(Comparator.comparingInt(ScoredChunk::score).reversed());
            return scored.stream().limit(topK).map(sc -> {
                sc.result.setScore(sc.score);
                return sc.result;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("LLM reranking failed, falling back to RRF order: {}", e.getMessage());
            return candidates.stream().limit(topK).collect(Collectors.toList());
        }
    }

    private String buildRerankPrompt(String query, String chunkContent) {
        return String.format("""
                You are a search relevance evaluation expert.
                Based on the following "user query" and "document excerpt", evaluate how relevant this
                document excerpt is to answering the user's query.

                User query: %s

                Document excerpt:
                %s

                Return only a single integer score (1-10), with no other content.
                10 - directly and perfectly answers | 7-9 - highly relevant | 4-6 - partially relevant | 1-3 - largely irrelevant
                """, query, truncateForRerank(chunkContent));
    }

    private String truncateForRerank(String content) {
        int maxLen = 1000;
        if (content == null) return "";
        return content.length() > maxLen ? content.substring(0, maxLen) : content;
    }

    private int parseRelevanceScore(String scoreText) {
        if (scoreText == null) return 5;
        Matcher m = RERANK_SCORE_PATTERN.matcher(scoreText.trim());
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 5; // Default to medium relevance
    }

    private record ScoredChunk(RagSearchResultVO result, int score) {}
}
