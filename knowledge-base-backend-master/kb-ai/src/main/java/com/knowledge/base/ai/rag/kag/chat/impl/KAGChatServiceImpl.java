package com.knowledge.base.ai.rag.kag.chat.impl;

import com.knowledge.base.ai.config.ModelProvider;
import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.rag.kag.chat.KAGChatService;
import com.knowledge.base.ai.rag.kag.retrieval.GraphContext;
import com.knowledge.base.ai.rag.kag.retrieval.HybridRetrievalService;
import com.knowledge.base.ai.rag.kag.retrieval.HybridRetrievalService.HybridResult;
import com.knowledge.base.ai.vo.CitationVO;
import com.knowledge.base.ai.vo.RagSearchResultVO;
import com.knowledge.base.ai.vo.ChatResponseVO;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * KAG-augmented chat service implementation
 *
 * <p>Combines RAG text retrieval and KAG graph reasoning, building an augmented
 * prompt that includes structured knowledge context and document excerpts, to
 * generate a well-grounded AI answer.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KAGChatServiceImpl implements KAGChatService
{

    private final HybridRetrievalService hybridRetrievalService;
    private final ModelProvider modelProvider;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    /** {@inheritDoc} */
    @Override
    public ChatResponseVO chatWithKnowledgeGraph(ChatRequestDTO requestDTO, Long userId) {
        log.info("KAG chat started: query='{}', userId={}", requestDTO.getContent(), userId);

        String query = requestDTO.getContent();
        String modelName = requestDTO.getModel();
        int topK = 5;
        boolean enableRerank = true;
        boolean enableKAG = requestDTO.isEnableKAG();

        // Step 1: Hybrid retrieval (RAG + KAG)
        HybridResult hybridResult = hybridRetrievalService.retrieveHybrid(
                query, topK, enableRerank, enableKAG, userId);

        // Step 2: Build KAG-enhanced prompt
        String prompt = buildKAGPrompt(query, hybridResult);

        // Step 3: LLM generation
        ChatLanguageModel model = getModel(modelName);
        String answer = model.generate(
                SystemMessage.from("You are an enterprise knowledge base assistant. Please answer the question by combining knowledge graph reasoning with document material."),
                UserMessage.from(prompt)
        ).content().text();

        // Step 4: Build citations
        List<CitationVO> citations = buildCitations(hybridResult.fusedResults());

        // Step 5: Build response
        ChatResponseVO response = ChatResponseVO.builder()
                .content(answer)
                .citations(citations)
                .fromKnowledgeBase(true)
                .graphContext(hybridResult.kagContext())
                .build();

        log.info("KAG chat completed: hasGraph={}, citations={}",
                hybridResult.knowledgeGraphEnhanced(), citations.size());
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public SseEmitter chatWithKnowledgeGraphStream(ChatRequestDTO requestDTO, Long userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30min timeout

        CompletableFuture.runAsync(() -> {
            try {
                String query = requestDTO.getContent();
                String modelName = requestDTO.getModel();
                boolean enableKAG = requestDTO.isEnableKAG();

                // Hybrid retrieval
                HybridResult hybridResult = hybridRetrievalService.retrieveHybrid(
                        query, 5, true, enableKAG, userId);

                // Build prompt and stream
                String prompt = buildKAGPrompt(query, hybridResult);
                ChatLanguageModel model = getModel(modelName);

                StringBuilder fullAnswer = new StringBuilder();
                model.generate(
                        SystemMessage.from("You are an enterprise knowledge base assistant. Please answer the question by combining knowledge graph reasoning with document material."),
                        UserMessage.from(prompt)
                ).content().text(); // non-streaming for now, use standard generate

                // Build response
                List<CitationVO> citations = buildCitations(hybridResult.fusedResults());
                ChatResponseVO response = ChatResponseVO.builder()
                        .content(fullAnswer.toString())
                        .citations(citations)
                        .fromKnowledgeBase(true)
                        .graphContext(hybridResult.kagContext())
                        .build();

                emitter.send(SseEmitter.event().name("message").data(response));
                emitter.complete();
            } catch (Exception e) {
                log.error("KAG stream chat failed: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ex) {
                    log.error("Failed to send error event", ex);
                }
                emitter.completeWithError(e);
            }
        }, asyncTaskExecutor);

        return emitter;
    }

    // ==================== Private Methods ====================

    /**
     * Build the KAG-augmented prompt (including knowledge graph reasoning paths + document excerpts)
     */
    private String buildKAGPrompt(String query, HybridResult hybridResult) {
        StringBuilder sb = new StringBuilder();

        // System instructions
        sb.append("You are an enterprise knowledge base assistant. Please answer the user's question by drawing on both of the following types of knowledge.\n");
        sb.append("When answering, please follow these rules:\n");
        sb.append("1. Prioritize the most direct reasoning path from the knowledge graph\n");
        sb.append("2. Use the reference document excerpts for specific details and verification\n");
        sb.append("3. When citing a reference document, mark it with a number like [1], [2]\n");
        sb.append("4. If the knowledge base has no relevant information, state this clearly\n\n");

        // KAG Graph Context
        GraphContext kagContext = hybridResult.kagContext();
        if (kagContext != null && kagContext.isHasResults()) {
            // Matched entities
            if (kagContext.getMatchedEntities() != null && !kagContext.getMatchedEntities().isEmpty()) {
                sb.append("=== Key Entities Retrieved from the Knowledge Graph ===\n");
                for (GraphContext.GraphEntity entity : kagContext.getMatchedEntities()) {
                    sb.append(String.format("- %s [%s] %s (connected to %d entities)\n",
                            entity.getName(), entity.getType(),
                            entity.getDescription() != null ? "- " + entity.getDescription() : "",
                            entity.getConnectionCount()));
                }
                sb.append("\n");
            }

            // Reasoning paths
            if (kagContext.getReasoningPaths() != null && !kagContext.getReasoningPaths().isEmpty()) {
                sb.append("=== Knowledge Graph Reasoning Paths ===\n");
                int pathIdx = 1;
                for (GraphContext.GraphPath path : kagContext.getReasoningPaths()) {
                    if (path.getNodes() == null || path.getNodes().size() < 2) continue;
                    sb.append("[Path ").append(pathIdx).append("] ");
                    for (int i = 0; i < path.getNodes().size(); i++) {
                        sb.append(path.getNodes().get(i));
                        if (i < path.getRelations().size()) {
                            sb.append(" →(").append(path.getRelations().get(i)).append(")→ ");
                        }
                    }
                    sb.append(" (").append(path.getHops()).append(" hops)\n");
                    pathIdx++;
                }
                sb.append("\n");
            }
        }

        // RAG/KAG text chunks
        List<RagSearchResultVO> fusedResults = hybridResult.fusedResults();
        if (fusedResults != null && !fusedResults.isEmpty()) {
            sb.append("=== Related Document Excerpts ===\n");
            for (int i = 0; i < fusedResults.size(); i++) {
                RagSearchResultVO chunk = fusedResults.get(i);
                sb.append(String.format("[%d] Source document: %s", i + 1, chunk.getDocumentTitle()));
                if (chunk.getHeading() != null && !chunk.getHeading().isEmpty()) {
                    sb.append(" | Section: ").append(chunk.getHeading());
                }
                sb.append(String.format(" | Relevance: %.2f", chunk.getScore()));
                sb.append("\n").append(chunk.getContent()).append("\n\n");
            }
        }

        // User query
        sb.append("=== User Question ===\n").append(query);

        return sb.toString();
    }

    private List<CitationVO> buildCitations(List<RagSearchResultVO> results) {
        if (results == null || results.isEmpty()) return List.of();

        List<CitationVO> citations = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            RagSearchResultVO result = results.get(i);
            String excerpt = result.getContent() != null &&
                    result.getContent().length() > 100
                    ? result.getContent().substring(0, 100) + "..."
                    : result.getContent();

            citations.add(CitationVO.builder()
                    .index(i + 1)
                    .documentId(result.getDocumentId())
                    .documentTitle(result.getDocumentTitle())
                    .excerpt(excerpt)
                    .relevanceScore(result.getScore())
                    .build());
        }
        return citations;
    }

    private ChatLanguageModel getModel(String modelName) {
        if (modelName != null && !modelName.isBlank()) {
            return modelProvider.getModel(modelName);
        }
        return modelProvider.getDefaultModel();
    }
}
