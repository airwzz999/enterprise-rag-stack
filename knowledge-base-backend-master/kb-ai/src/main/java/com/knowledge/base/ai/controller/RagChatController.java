package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.rag.service.RagChatService;
import com.knowledge.base.ai.vo.ChatResponseVO;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG chat controller
 *
 * <p>Provides retrieval-augmented AI chat APIs based on the knowledge base (synchronous + SSE streaming).</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/rag/chat")
@RequiredArgsConstructor
@Tag(name = "RAG Chat", description = "Knowledge-base-based AI chat APIs")
public class RagChatController {

    private final RagChatService ragChatService;

    /**
     * RAG chat (synchronous)
     *
     * @param requestDTO chat request
     * @param request    HTTP request
     * @return chat response (including citation sources)
     */
    @PostMapping
    @Operation(summary = "RAG chat", description = "Retrieval-augmented AI chat based on the knowledge base")
    public Result<ChatResponseVO> chat(@Valid @RequestBody ChatRequestDTO requestDTO,
                                        HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        log.info("RAG chat request: userId={}, contentLength={}", userId, requestDTO.getContent().length());
        ChatResponseVO response = ragChatService.chatWithContext(requestDTO, userId);
        return Result.success(response);
    }

    /**
     * RAG chat (SSE streaming)
     *
     * @param requestDTO chat request
     * @param request    HTTP request
     * @return SSE event stream
     */
    @PostMapping("/stream")
    @Operation(summary = "RAG streaming chat", description = "Retrieval-augmented AI streaming chat based on the knowledge base")
    public SseEmitter chatStream(@Valid @RequestBody ChatRequestDTO requestDTO,
                                  HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        log.info("RAG streaming chat request: userId={}, contentLength={}", userId, requestDTO.getContent().length());
        return ragChatService.chatWithContextStream(requestDTO, userId);
    }

}
