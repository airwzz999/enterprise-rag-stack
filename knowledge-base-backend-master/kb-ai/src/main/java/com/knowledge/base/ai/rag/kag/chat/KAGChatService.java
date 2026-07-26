package com.knowledge.base.ai.rag.kag.chat;

import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.vo.ChatResponseVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * KAG-augmented chat service interface
 *
 * <p>Combines RAG (text retrieval) and KAG (knowledge graph reasoning) for augmented chat.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface KAGChatService {

    /**
     * KAG-augmented synchronous chat
     *
     * @param requestDTO chat request (must set enableKAG=true)
     * @param userId     current user ID
     * @return chat response including graph context
     */
    ChatResponseVO chatWithKnowledgeGraph(ChatRequestDTO requestDTO, Long userId);

    /**
     * KAG-augmented streaming chat (SSE)
     *
     * @param requestDTO chat request
     * @param userId     current user ID
     * @return SSE event stream
     */
    SseEmitter chatWithKnowledgeGraphStream(ChatRequestDTO requestDTO, Long userId);
}
