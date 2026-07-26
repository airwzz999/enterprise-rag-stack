package com.knowledge.base.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.entity.Conversation;
import com.knowledge.base.ai.entity.Message;
import com.knowledge.base.ai.mapper.ConversationMapper;
import com.knowledge.base.ai.mapper.MessageMapper;
import com.knowledge.base.ai.service.AiConversationService;
import com.knowledge.base.ai.vo.ConversationVO;
import com.knowledge.base.ai.vo.MessageVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI conversation management service implementation
 *
 * <p>Designed following the Alibaba Java Development Guidelines, implementing
 * conversation management business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class AiConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements AiConversationService {

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private MessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConversation(ChatRequestDTO requestDTO, Long userId) {
        log.info("Creating conversation: userId={}", userId);

        // Build the conversation entity, using the user's first question as the title
        String title = requestDTO.getContent();
        if (title != null && title.length() > 30) {
            title = title.substring(0, 30);
        }
        Conversation conversation = new Conversation();
        conversation.setTitle(title != null ? title : "New Conversation");
        conversation.setUserId(userId);
        conversation.setModel(requestDTO.getModel() != null ? requestDTO.getModel() : "qwen"); // Default model
        conversation.setSystemPrompt(requestDTO.getSystemPrompt());
        conversation.setTokensUsed(0);
        conversation.setMessageCount(0);
        conversation.setStatus(0); // 0 - in progress
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversation.setDeleted(0);

        // Save the conversation
        int count = conversationMapper.insert(conversation);
        if (count <= 0) {
            throw new RuntimeException("Failed to create conversation");
        }

        log.info("Conversation created successfully: conversationId={}", conversation.getId());
        return conversation.getId();
    }

    /** {@inheritDoc} */
    @Override
    public ConversationVO getConversation(Long conversationId, Long userId) {
        log.info("Getting conversation details: conversationId={}, userId={}", conversationId, userId);

        if (conversationId == null) {
            throw new RuntimeException("Conversation ID must not be null");
        }

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        // Verify user permission
        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to access this conversation");
        }

        ConversationVO vo = convertToVO(conversation);

        // Load conversation messages
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt)
        );
        vo.setMessages(messages.stream().map(this::convertMessageToVO).collect(Collectors.toList()));

        return vo;
    }

    /** {@inheritDoc} */
    @Override
    public IPage<ConversationVO> listConversations(Long userId, Long current, Long size) {
        log.info("Getting conversation list: userId={}, current={}, size={}", userId, current, size);

        // Build the query conditions
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getUpdatedAt);

        // Paginated query
        Page<Conversation> page = new Page<>(current, size);
        IPage<Conversation> conversationPage = conversationMapper.selectPage(page, wrapper);

        // Convert to VO
        return conversationPage.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConversation(Long conversationId, Long userId) {
        log.info("Deleting conversation: conversationId={}, userId={}", conversationId, userId);

        if (conversationId == null) {
            throw new RuntimeException("Conversation ID must not be null");
        }

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        // Verify user permission
        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to delete this conversation");
        }

        // Delete the conversation
        int count = conversationMapper.deleteById(conversationId);

        // TODO: cascade-delete related messages

        log.info("Conversation deleted successfully: conversationId={}", conversationId);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long conversationId, Integer status) {
        log.info("Updating conversation status: conversationId={}, status={}", conversationId, status);

        if (conversationId == null) {
            throw new RuntimeException("Conversation ID must not be null");
        }

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        conversation.setStatus(status);
        conversation.setUpdatedAt(LocalDateTime.now());

        int count = conversationMapper.updateById(conversation);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTokens(Long conversationId, Integer tokens) {
        log.info("Updating conversation token usage: conversationId={}, tokens={}", conversationId, tokens);

        if (conversationId == null) {
            throw new RuntimeException("Conversation ID must not be null");
        }

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        conversation.setTokensUsed((conversation.getTokensUsed() != null ? conversation.getTokensUsed() : 0) + tokens);
        conversation.setUpdatedAt(LocalDateTime.now());

        int count = conversationMapper.updateById(conversation);
        return count > 0;
    }

    /**
     * Update the conversation title (called after the first exchange)
     *
     * @param conversationId conversation ID
     * @param title          the title
     * @return whether the operation succeeded
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTitle(Long conversationId, String title) {
        log.info("Updating conversation title: conversationId={}, title={}", conversationId, title);

        if (conversationId == null) {
            throw new RuntimeException("Conversation ID must not be null");
        }

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        conversation.setTitle(title);
        conversation.setUpdatedAt(LocalDateTime.now());

        int count = conversationMapper.updateById(conversation);
        return count > 0;
    }

    /**
     * Increment the message count
     *
     * @param conversationId conversation ID
     * @param count          the amount to increment by
     * @return whether the operation succeeded
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementMessageCount(Long conversationId, Integer count) {
        log.info("Incrementing message count: conversationId={}, count={}", conversationId, count);

        if (conversationId == null) {
            throw new RuntimeException("Conversation ID must not be null");
        }

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        conversation.setMessageCount((conversation.getMessageCount() != null ? conversation.getMessageCount() : 0) + count);
        conversation.setUpdatedAt(LocalDateTime.now());

        int updateCount = conversationMapper.updateById(conversation);
        return updateCount > 0;
    }

    /**
     * Convert a message entity to a VO
     *
     * @param message the message entity
     * @return the message VO
     */
    private MessageVO convertMessageToVO(Message message) {
        return MessageVO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .role(message.getRole())
                .content(message.getContent())
                .tokens(message.getTokens())
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * Convert to a VO
     *
     * @param conversation the conversation entity
     * @return the conversation VO
     */
    private ConversationVO convertToVO(Conversation conversation) {
        return ConversationVO.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .model(conversation.getModel())
                .tokensUsed(conversation.getTokensUsed())
                .messageCount(conversation.getMessageCount())
                .status(conversation.getStatus())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConversation(String title, Long userId) {
        log.info("Creating conversation (via title): title={}, userId={}", title, userId);

        Conversation conversation = new Conversation();
        conversation.setTitle(title);
        conversation.setUserId(userId);
        conversation.setModel("qwen");
        conversation.setTokensUsed(0);
        conversation.setMessageCount(0);
        conversation.setStatus(0);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversation.setDeleted(0);

        int count = conversationMapper.insert(conversation);
        if (count <= 0) {
            throw new RuntimeException("Failed to create conversation");
        }

        log.info("Conversation created successfully: conversationId={}", conversation.getId());
        return conversation.getId();
    }

    /** {@inheritDoc} */
    @Override
    public Conversation getById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }
}
