package com.knowledge.base.ai.rag.service.impl;

import com.knowledge.base.ai.config.ModelProvider;
import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.entity.Conversation;
import com.knowledge.base.ai.entity.Message;
import com.knowledge.base.ai.mapper.MessageMapper;
import com.knowledge.base.ai.config.RagProperties;
import com.knowledge.base.ai.rag.service.RagChatService;
import com.knowledge.base.ai.rag.service.RagRetrievalService;
import com.knowledge.base.ai.vo.CitationVO;
import com.knowledge.base.ai.vo.RagSearchResultVO;
import com.knowledge.base.ai.service.AiConversationService;
import com.knowledge.base.ai.vo.ChatResponseVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RAG chat service implementation
 *
 * <p>Implements the core knowledge base Q&amp;A pipeline:
 * <ol>
 *   <li>Retrieve relevant document chunks from ES</li>
 *   <li>Build the RAG prompt (reference material + user question)</li>
 *   <li>Call the LLM to generate an answer</li>
 *   <li>Parse citation markers [1] [2]</li>
 *   <li>Persist messages to MySQL</li>
 * </ol>
 * Built-in graceful degradation: falls back to plain LLM chat when retrieval
 * fails or RAG is disabled.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    private final RagRetrievalService ragRetrievalService;
    private final ModelProvider modelProvider;
    private final RagProperties ragProperties;
    private final MessageMapper messageMapper;
    private final AiConversationService conversationService;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    /** {@inheritDoc} */
    @Override
    public ChatResponseVO chatWithContext(ChatRequestDTO requestDTO, Long userId) {
        // 1. Create or validate the conversation
        Long conversationId = prepareConversation(requestDTO, userId);

        // 2. Retrieve relevant knowledge
        List<RagSearchResultVO> context = safeRetrieve(requestDTO.getContent());

        // 3. Build the RAG prompt
        String prompt = buildRagPrompt(requestDTO.getContent(), context);
        String modelName = requestDTO.getModel() != null ? requestDTO.getModel() : modelProvider.getDefaultModelName();

        // 4. Build the chat context (history messages) to support memory
        ChatLanguageModel chatModel = modelProvider.getModel(modelName);
        List<ChatMessage> chatMessages = buildChatHistory(conversationId);

        // 5. Persist the user message
        Message userMsgEntity = Message.builder()
                .conversationId(conversationId)
                .role("user")
                .content(requestDTO.getContent())
                .tokens(estimateTokens(requestDTO.getContent()))
                .build();
        messageMapper.insert(userMsgEntity);

        // 6. Add the RAG prompt to the context and call the LLM
        chatMessages.add(UserMessage.from(prompt));
        Response<AiMessage> response = chatModel.generate(chatMessages);
        String responseContent = response.content().text();

        // 7. Build the citation list
        List<CitationVO> citations = buildCitations(context, responseContent);

        Message aiMsgEntity = Message.builder()
                .conversationId(conversationId)
                .role("assistant")
                .content(responseContent)
                .tokens(estimateTokens(responseContent))
                .build();
        messageMapper.insert(aiMsgEntity);

        conversationService.updateTokens(conversationId,
                userMsgEntity.getTokens() + aiMsgEntity.getTokens());

        Conversation conv = conversationService.getById(conversationId);

        return ChatResponseVO.builder()
                .conversationId(conversationId)
                .messageId(aiMsgEntity.getId())
                .content(responseContent)
                .tokens(userMsgEntity.getTokens() + aiMsgEntity.getTokens())
                .title(conv != null ? conv.getTitle() : "New Conversation")
                .citations(citations)
                .fromKnowledgeBase(!context.isEmpty())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public SseEmitter chatWithContextStream(ChatRequestDTO requestDTO, Long userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        try {
            Long conversationId = prepareConversation(requestDTO, userId);
            List<RagSearchResultVO> context = safeRetrieve(requestDTO.getContent());
            String prompt = buildRagPrompt(requestDTO.getContent(), context);
            String modelName = requestDTO.getModel() != null ? requestDTO.getModel() : modelProvider.getDefaultModelName();

            CompletableFuture.runAsync(() -> {
                try {
                    // Use the streaming model for token-by-token SSE output
                    StreamingChatLanguageModel streamingModel = modelProvider.getStreamingModel(modelName);

                    // Build the chat context (history messages) to support memory
                    List<ChatMessage> chatMessages = buildChatHistory(conversationId);

                    // Save the user message first (before streaming starts, independent of retrieved context/output)
                    Message userMsgEntity = Message.builder()
                            .conversationId(conversationId)
                            .role("user")
                            .content(requestDTO.getContent())
                            .tokens(estimateTokens(requestDTO.getContent()))
                            .build();
                    messageMapper.insert(userMsgEntity);

                    // Add the RAG prompt to the context
                    chatMessages.add(UserMessage.from(prompt));

                    // Use a StringBuilder to accumulate the full response for final persistence
                    StringBuilder fullResponseBuilder = new StringBuilder();

                    streamingModel.generate(chatMessages, new StreamingResponseHandler<AiMessage>() {
                        @Override
                        public void onNext(String token) {
                            fullResponseBuilder.append(token);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(token));
                            } catch (IOException e) {
                                log.warn("Failed to send streaming token (client may have disconnected): {}", e.getMessage());
                            }
                        }

                        @Override
                        public void onComplete(Response<AiMessage> response) {
                            try {
                                String fullResponse = fullResponseBuilder.toString();

                                List<CitationVO> citations = buildCitations(context, fullResponse);

                                Message aiMsgEntity = Message.builder()
                                        .conversationId(conversationId)
                                        .role("assistant")
                                        .content(fullResponse)
                                        .tokens(estimateTokens(fullResponse))
                                        .build();
                                messageMapper.insert(aiMsgEntity);

                                AtomicInteger totalTokens = new AtomicInteger(userMsgEntity.getTokens());
                                totalTokens.addAndGet(aiMsgEntity.getTokens());
                                conversationService.updateTokens(conversationId, totalTokens.get());

                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(ChatResponseVO.builder()
                                                .conversationId(conversationId)
                                                .messageId(aiMsgEntity.getId())
                                                .content(fullResponse)
                                                .tokens(totalTokens.get())
                                                .citations(citations)
                                                .fromKnowledgeBase(!context.isEmpty())
                                                .build()));
                                emitter.complete();
                            } catch (IOException e) {
                                log.warn("Failed to send completion event (client may have disconnected): {}", e.getMessage());
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("RAG streaming chat failed: {}", error.getMessage(), error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(error.getMessage() != null ? error.getMessage() : "AI service call failed"));
                                emitter.complete();
                            } catch (IOException ioException) {
                                log.error("Failed to send error event: {}", ioException.getMessage());
                            }
                        }
                    });

                } catch (Exception e) {
                    log.error("RAG streaming chat failed: {}", e.getMessage(), e);
                    try {
                        emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                        emitter.complete();
                    } catch (IOException ioException) {
                        log.error("Failed to send error event: {}", ioException.getMessage());
                    }
                }
            }, asyncTaskExecutor);

        } catch (Exception e) {
            log.error("Failed to create SSE emitter: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Safe retrieval: catches exceptions and returns an empty list without interrupting the main flow
     */
    private List<RagSearchResultVO> safeRetrieve(String query) {
        try {
            return ragRetrievalService.retrieve(query,
                    ragProperties.getRetrieval().getDefaultTopK(),
                    ragProperties.getRerank().isEnabled());
        } catch (Exception e) {
            log.warn("RAG retrieval failed, falling back to plain LLM answer: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Build the RAG prompt
     */
    private String buildRagPrompt(String query, List<RagSearchResultVO> context) {
        if (context.isEmpty()) {
            return query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("You are an enterprise knowledge base assistant. Please answer the user's question based only on the following reference material.\n");
        sb.append("If the reference material has no relevant information, state that [this question cannot currently be answered based on the existing knowledge base]; never fabricate content.\n");
        sb.append("When answering, mark citations at the end of the referenced material with a number, e.g. [1], [2].\n");
        sb.append("The answer should be professional, accurate, and concise.\n\n");

        sb.append("=== Reference Material ===\n");
        for (int i = 0; i < context.size(); i++) {
            RagSearchResultVO chunk = context.get(i);
            sb.append(String.format("[%d] Source document: %s", i + 1, chunk.getDocumentTitle()));
            if (chunk.getHeading() != null && !chunk.getHeading().isEmpty()) {
                sb.append(" | Section: ").append(chunk.getHeading());
            }
            sb.append("\n").append(chunk.getContent()).append("\n\n");
        }

        sb.append("=== User Question ===\n");
        sb.append(query);
        return sb.toString();
    }

    /**
     * Build the citation list
     */
    private List<CitationVO> buildCitations(List<RagSearchResultVO> context, String response) {
        if (context.isEmpty()) {
            return List.of();
        }
        List<CitationVO> citations = new ArrayList<>();
        for (int i = 0; i < context.size(); i++) {
            RagSearchResultVO chunk = context.get(i);
            String excerpt = chunk.getContent();
            if (excerpt != null && excerpt.length() > 100) {
                excerpt = excerpt.substring(0, 100) + "...";
            }
            citations.add(CitationVO.builder()
                    .index(i + 1)
                    .documentId(chunk.getDocumentId())
                    .documentTitle(chunk.getDocumentTitle())
                    .excerpt(excerpt)
                    .relevanceScore(chunk.getScore())
                    .build());
        }
        return citations;
    }

    /**
     * Prepare the conversation (create a new one or validate an existing one)
     */
    private Long prepareConversation(ChatRequestDTO requestDTO, Long userId) {
        ChatRequestDTO createDTO = ChatRequestDTO.builder()
                .content(requestDTO.getContent())
                .model(requestDTO.getModel())
                .systemPrompt(requestDTO.getSystemPrompt())
                .build();

        if (requestDTO.getConversationId() == null) {
            return conversationService.createConversation(createDTO, userId);
        }
        Conversation existing = conversationService.getById(requestDTO.getConversationId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            return conversationService.createConversation(createDTO, userId);
        }
        return existing.getId();
    }

    /**
     * Load the most recent N conversation records from MySQL to build the LangChain4j message context
     * <p>Implements chat memory, letting the AI understand the history of the current conversation (up to the most recent 20 messages)</p>
     */
    private List<ChatMessage> buildChatHistory(Long conversationId) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getConversationId, conversationId)
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT 20");
        List<Message> historyMessages = messageMapper.selectList(queryWrapper);
        // Sort in chronological order
        Collections.reverse(historyMessages);

        List<ChatMessage> chatMessages = new ArrayList<>();
        for (Message msg : historyMessages) {
            if ("user".equals(msg.getRole())) {
                chatMessages.add(UserMessage.from(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                chatMessages.add(AiMessage.from(msg.getContent()));
            }
        }
        return chatMessages;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseChars = text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        int otherChars = text.length() - chineseChars;
        return chineseChars + (otherChars / 4);
    }
}
