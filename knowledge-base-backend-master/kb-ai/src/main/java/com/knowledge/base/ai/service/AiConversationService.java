package com.knowledge.base.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.entity.Conversation;
import com.knowledge.base.ai.vo.ConversationVO;

/**
 * AI conversation management service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface AiConversationService {

    /**
     * Create a conversation (via ChatRequestDTO)
     *
     * @param requestDTO chat request
     * @param userId     user ID
     * @return conversation ID
     */
    Long createConversation(ChatRequestDTO requestDTO, Long userId);

    /**
     * Create a conversation (via title)
     *
     * @param title  conversation title
     * @param userId user ID
     * @return conversation ID
     */
    Long createConversation(String title, Long userId);

    /**
     * Get conversation details
     *
     * @param conversationId conversation ID
     * @param userId         user ID
     * @return conversation details
     */
    ConversationVO getConversation(Long conversationId, Long userId);

    /**
     * Get the list of conversations
     *
     * @param userId    user ID
     * @param current   current page
     * @param size      page size
     * @return list of conversations
     */
    IPage<ConversationVO> listConversations(Long userId, Long current, Long size);

    /**
     * Delete a conversation
     *
     * @param conversationId conversation ID
     * @param userId         user ID
     * @return whether the operation succeeded
     */
    boolean deleteConversation(Long conversationId, Long userId);

    /**
     * Update conversation status
     *
     * @param conversationId conversation ID
     * @param status         status
     * @return whether the operation succeeded
     */
    boolean updateStatus(Long conversationId, Integer status);

    /**
     * Update the conversation's token count
     *
     * @param conversationId conversation ID
     * @param tokens         token count
     * @return whether the operation succeeded
     */
    boolean updateTokens(Long conversationId, Integer tokens);

    /**
     * Get a conversation entity by ID
     *
     * @param conversationId conversation ID
     * @return the conversation entity
     */
    Conversation getById(Long conversationId);
}
