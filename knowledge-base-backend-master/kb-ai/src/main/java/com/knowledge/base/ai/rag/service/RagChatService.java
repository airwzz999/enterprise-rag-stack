package com.knowledge.base.ai.rag.service;

import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.vo.ChatResponseVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG chat service interface
 *
 * <p>Supports both synchronous and SSE streaming modes, each with a built-in
 * retrieve → build prompt → LLM generate pipeline and graceful degradation.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface RagChatService {

    /**
     * Knowledge-base-based chat (synchronous)
     *
     * @param requestDTO chat request
     * @param userId     user ID
     * @return chat response (including citation sources)
     */
    ChatResponseVO chatWithContext(ChatRequestDTO requestDTO, Long userId);

    /**
     * Knowledge-base-based chat (SSE streaming)
     *
     * @param requestDTO chat request
     * @param userId     user ID
     * @return SSE emitter
     */
    SseEmitter chatWithContextStream(ChatRequestDTO requestDTO, Long userId);
}
