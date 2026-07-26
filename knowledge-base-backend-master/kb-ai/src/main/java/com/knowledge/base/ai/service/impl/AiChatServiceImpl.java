package com.knowledge.base.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.ai.config.ModelProvider;
import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.entity.Conversation;
import com.knowledge.base.ai.entity.Message;
import com.knowledge.base.ai.mapper.ConversationMapper;
import com.knowledge.base.ai.mapper.MessageMapper;
import com.knowledge.base.ai.rag.service.RagChatService;
import com.knowledge.base.ai.service.AiChatService;
import com.knowledge.base.ai.service.AiConversationService;
import com.knowledge.base.ai.vo.ChatResponseVO;
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
import org.springframework.beans.factory.annotation.Autowired;
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
 * AI chat service implementation
 *
 * <p>Implements the business logic for AI chat, supporting RAG routing and graceful degradation</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
        implements AiChatService {

    private final ModelProvider modelProvider;
    private final MessageMapper messageMapper;
    private final AiConversationService conversationService;

    @Autowired(required = false)
    private RagChatService ragChatService;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    /** {@inheritDoc} */
    @Override
    public ChatResponseVO chat(ChatRequestDTO requestDTO, Long userId) {
        // RAG routing: if knowledge base retrieval augmentation is enabled and the RAG service is available
        if (requestDTO.isEnableRag() && ragChatService != null) {
            try {
                return ragChatService.chatWithContext(requestDTO, userId);
            } catch (Exception e) {
                log.warn("RAG chat failed, falling back to plain LLM chat: {}", e.getMessage());
                // Graceful degradation: continue with the plain chat flow
            }
        }

        final String modelName = requestDTO.getModel() != null ? requestDTO.getModel() : modelProvider.getDefaultModelName();
        try {
            Long conversationId = requestDTO.getConversationId();
            if (conversationId == null) {
                conversationId = conversationService.createConversation(requestDTO, userId);
            } else {
                // Verify the conversation exists and belongs to this user, otherwise create a new one
                Conversation existingConv = conversationService.getById(conversationId);
                if (existingConv == null || !existingConv.getUserId().equals(userId)) {
                    log.warn("Conversation not found or access denied, creating a new conversation: conversationId={}, userId={}", conversationId, userId);
                    conversationId = conversationService.createConversation(requestDTO, userId);
                } else {
                    // If the title is still the default "New Conversation", update it from the first message
                    updateConversationTitle(existingConv, requestDTO.getContent());
                }
            }

            // Select the corresponding ChatLanguageModel based on the model specified in the request
            ChatLanguageModel chatModel = modelProvider.getModel(requestDTO.getModel());

            log.info("Initiating AI request: model={}, conversationId={}, contentLength={}",
                    modelName, conversationId, requestDTO.getContent().length());

            // Build the chat context (history messages + current message) to support memory
            List<ChatMessage> chatMessages = buildChatHistory(conversationId);

            // Save the user message
            Message userMsgEntity = Message.builder()
                    .conversationId(conversationId)
                    .role("user")
                    .content(requestDTO.getContent())
                    .tokens(estimateTokens(requestDTO.getContent()))
                    .build();
            messageMapper.insert(userMsgEntity);

            // Add the current user message to the context
            chatMessages.add(UserMessage.from(requestDTO.getContent()));

            Response<AiMessage> response = chatModel.generate(chatMessages);
            String responseContent = response.content().text();

            Message aiMsgEntity = Message.builder()
                    .conversationId(conversationId)
                    .role("assistant")
                    .content(responseContent)
                    .tokens(estimateTokens(responseContent))
                    .build();
            messageMapper.insert(aiMsgEntity);

            int totalTokens = userMsgEntity.getTokens() + aiMsgEntity.getTokens();
            conversationService.updateTokens(conversationId, totalTokens);

            Conversation conversation = conversationService.getById(conversationId);
            String title = conversation != null ? conversation.getTitle() : "New Conversation";

            log.info("AI chat completed: model={}, conversationId={}, tokens={}", modelName, conversationId, totalTokens);

            return ChatResponseVO.builder()
                    .conversationId(conversationId)
                    .messageId(aiMsgEntity.getId())
                    .content(responseContent)
                    .tokens(totalTokens)
                    .title(title)
                    .build();

        } catch (Exception e) {
            log.error("AI chat failed: {}", e.getMessage(), e);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                Throwable cause = e.getCause();
                String causeMsg = (cause != null) ? cause.getMessage() : null;
                if (causeMsg != null && !causeMsg.isEmpty()) {
                    errorMsg = causeMsg;
                } else {
                    errorMsg = "AI service call failed (model: " + modelName + "), please check the API Key configuration and network connection";
                }
            }
            throw new RuntimeException("AI chat failed: " + errorMsg);
        }
    }

    /** {@inheritDoc} */
    @Override
    public SseEmitter chatStream(ChatRequestDTO requestDTO, Long userId) {
        // RAG routing: if knowledge base retrieval augmentation is enabled and the RAG service is available
        if (requestDTO.isEnableRag() && ragChatService != null) {
            try {
                return ragChatService.chatWithContextStream(requestDTO, userId);
            } catch (Exception e) {
                log.warn("RAG streaming chat failed, falling back to plain LLM streaming chat: {}", e.getMessage());
                // Graceful degradation: continue with plain streaming chat
            }
        }

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        try {
            final Long conversationIdFinal = requestDTO.getConversationId();
            Long conversationId = conversationIdFinal;
            if (conversationId == null) {
                conversationId = conversationService.createConversation(requestDTO, userId);
            } else {
                // Verify the conversation exists and belongs to this user, otherwise create a new one
                Conversation existingConv = conversationService.getById(conversationId);
                if (existingConv == null || !existingConv.getUserId().equals(userId)) {
                    log.warn("Conversation not found or access denied, creating a new conversation: conversationId={}, userId={}", conversationId, userId);
                    conversationId = conversationService.createConversation(requestDTO, userId);
                } else {
                    // If the title is still the default "New Conversation", update it from the first message
                    updateConversationTitle(existingConv, requestDTO.getContent());
                }
            }
            final Long finalConversationId = conversationId;

            // Determine the model name before running asynchronously
            final String modelName = requestDTO.getModel() != null ? requestDTO.getModel() : modelProvider.getDefaultModelName();

            CompletableFuture.runAsync(() -> {
                try {
                    // Use the streaming model for token-by-token SSE output
                    StreamingChatLanguageModel streamingModel = modelProvider.getStreamingModel(modelName);

                    log.info("Initiating AI streaming request: model={}, conversationId={}, contentLength={}",
                            modelName, finalConversationId, requestDTO.getContent().length());

                    // Build the chat context (history messages) to support memory
                    List<ChatMessage> chatMessages = buildChatHistory(finalConversationId);

                    // Save the user message first (before streaming starts)
                    Message userMsgEntity = Message.builder()
                            .conversationId(finalConversationId)
                            .role("user")
                            .content(requestDTO.getContent())
                            .tokens(estimateTokens(requestDTO.getContent()))
                            .build();
                    messageMapper.insert(userMsgEntity);

                    // Add the current user message to the context
                    chatMessages.add(UserMessage.from(requestDTO.getContent()));

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

                                Message aiMsgEntity = Message.builder()
                                        .conversationId(finalConversationId)
                                        .role("assistant")
                                        .content(fullResponse)
                                        .tokens(estimateTokens(fullResponse))
                                        .build();
                                messageMapper.insert(aiMsgEntity);

                                AtomicInteger totalTokens = new AtomicInteger(userMsgEntity.getTokens());
                                totalTokens.addAndGet(aiMsgEntity.getTokens());
                                conversationService.updateTokens(finalConversationId, totalTokens.get());

                                log.info("AI streaming chat completed: model={}, conversationId={}", modelName, finalConversationId);

                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(ChatResponseVO.builder()
                                                .conversationId(finalConversationId)
                                                .messageId(aiMsgEntity.getId())
                                                .content(fullResponse)
                                                .tokens(totalTokens.get())
                                                .build()));
                                emitter.complete();
                            } catch (IOException e) {
                                log.warn("Failed to send completion event (client may have disconnected): {}", e.getMessage());
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("Streaming chat failed [model={}]: {}", modelName, error.getMessage(), error);
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
                    // Extract the HTTP status code from OpenAiHttpException
                    String httpCodeInfo = "";
                    Throwable root = e;
                    while (root != null) {
                        if (root instanceof dev.ai4j.openai4j.OpenAiHttpException oahe) {
                            try {
                                int code = (int) oahe.getClass().getMethod("code").invoke(oahe);
                                httpCodeInfo = " HTTP_" + code;
                            } catch (Exception ignored) {}
                            try {
                                String json = (String) oahe.getClass().getMethod("json").invoke(oahe);
                                if (json != null && !json.isEmpty()) {
                                    httpCodeInfo += " Response: " + json;
                                }
                            } catch (Exception ignored) {}
                            break;
                        }
                        root = root.getCause();
                    }
                    log.error("Streaming chat failed [model={}]:{}{}", modelName,
                            httpCodeInfo.isEmpty() ? "" : httpCodeInfo,
                            e.getMessage() != null ? " " + e.getMessage() : "", e);

                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        Throwable cause = e.getCause();
                        String causeMsg = (cause != null) ? cause.getMessage() : null;
                        if (causeMsg != null && !causeMsg.isEmpty()) {
                            errorMsg = causeMsg;
                        } else {
                            String detail = "AI service call failed (model: " + modelName + ")" + httpCodeInfo;
                            if (httpCodeInfo.contains("404")) {
                                detail += " - please verify the API Key is valid and the Base URL is configured correctly";
                            } else if (httpCodeInfo.contains("401") || httpCodeInfo.contains("403")) {
                                detail += " - please verify the API Key is valid";
                            }
                            errorMsg = detail;
                        }
                    }
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(errorMsg));
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

    /** {@inheritDoc} */
    @Override
    public String getConversationHistory(Long conversationId, Long userId) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getCreatedAt);

        List<Message> messages = messageMapper.selectList(queryWrapper);

        StringBuilder history = new StringBuilder();
        for (Message message : messages) {
            history.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }

        return history.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String generateTitle(String firstMessage) {
        try {
            String prompt = "Based on the following conversation content, generate a short title (no more than 10 words):\n" + firstMessage;
            UserMessage userMessage = UserMessage.from(prompt);
            // Use the default model for title generation
            ChatLanguageModel chatModel = modelProvider.getDefaultModel();
            Response<AiMessage> response = chatModel.generate(userMessage);
            return response.content().text().trim();
        } catch (Exception e) {
            log.error("Failed to generate conversation title: {}", e.getMessage(), e);
            return "New Conversation";
        }
    }

    /**
     * If the conversation title is still the default value (starting with "New Conversation"),
     * update it from the content of the first message
     */
    private void updateConversationTitle(Conversation conversation, String content) {
        String currentTitle = conversation.getTitle();
        if (currentTitle != null && currentTitle.startsWith("New Conversation") && content != null && !content.isEmpty()) {
            String newTitle = content.length() > 30 ? content.substring(0, 30) : content;
            conversation.setTitle(newTitle);
            conversation.setUpdatedAt(LocalDateTime.now());
            this.updateById(conversation);
            log.info("Updated conversation title: conversationId={}, title={}", conversation.getId(), newTitle);
        }
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
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseChars = text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        int otherChars = text.length() - chineseChars;

        return chineseChars + (otherChars / 4);
    }
}
