package com.knowledge.base.ai.service;

import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.vo.ChatResponseVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI chat service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface AiChatService {

    /**
     * AI chat (non-streaming)
     *
     * @param requestDTO chat request
     * @param userId     user ID
     * @return chat response
     */
    ChatResponseVO chat(ChatRequestDTO requestDTO, Long userId);

    /**
     * AI chat (streaming)
     *
     * @param requestDTO chat request
     * @param userId     user ID
     * @return SSE event emitter
     */
    SseEmitter chatStream(ChatRequestDTO requestDTO, Long userId);

    /**
     * Get conversation history
     *
     * @param conversationId conversation ID
     * @param userId         user ID
     * @return conversation history
     */
    String getConversationHistory(Long conversationId, Long userId);

    /**
     * Generate a conversation title
     *
     * @param firstMessage the content of the first message
     * @return the conversation title
     */
    String generateTitle(String firstMessage);
}
