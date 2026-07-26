package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.rag.kag.chat.KAGChatService;
import com.knowledge.base.ai.rag.kag.retrieval.KAGRetrievalService;
import com.knowledge.base.ai.rag.kag.retrieval.GraphContext;
import com.knowledge.base.ai.vo.ChatResponseVO;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * KAG (knowledge graph augmented) chat controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/kag")
@Tag(name = "KAG Knowledge Graph Chat", description = "Augmented chat and retrieval APIs based on the Neo4j knowledge graph")
public class KAGChatController {

    @Resource
    private KAGChatService kagChatService;

    @Resource
    private KAGRetrievalService kagRetrievalService;

    @PostMapping("/chat")
    @Operation(summary = "KAG-augmented chat", description = "Augmented chat combining RAG text retrieval with KAG knowledge graph reasoning")
    public Result<ChatResponseVO> chat(@RequestBody @Valid ChatRequestDTO requestDTO,
                                        HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        ChatResponseVO response = kagChatService.chatWithKnowledgeGraph(requestDTO, userId);
        return Result.success("KAG chat completed", response);
    }

    @PostMapping("/chat/stream")
    @Operation(summary = "KAG-augmented streaming chat", description = "Knowledge-graph-augmented streaming chat (SSE)")
    public SseEmitter chatStream(@RequestBody @Valid ChatRequestDTO requestDTO,
                                  HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        return kagChatService.chatWithKnowledgeGraphStream(requestDTO, userId);
    }

    @PostMapping("/search")
    @Operation(summary = "KAG graph retrieval", description = "Retrieve structured knowledge and reasoning paths from the knowledge graph")
    public Result<GraphContext> search(
            @Parameter(description = "Query text", required = true) @RequestParam String query,
            @Parameter(description = "Maximum number of entities") @RequestParam(defaultValue = "10") int maxEntities,
            @Parameter(description = "Maximum number of hops") @RequestParam(defaultValue = "2") int maxHops,
            @Parameter(description = "Maximum number of text chunks") @RequestParam(defaultValue = "15") int maxChunks) {

        GraphContext context = kagRetrievalService.retrieveGraphContext(
                query, maxEntities, maxHops, maxChunks);
        return Result.success(context);
    }
}
