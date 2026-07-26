package com.knowledge.base.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI chat request DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI chat request parameters")
public class ChatRequestDTO {

    /**
     * Conversation ID (empty for the first message in a conversation)
     */
    @Schema(description = "Conversation ID")
    private Long conversationId;

    /**
     * User message
     */
    @Schema(description = "User message")
    @NotBlank(message = "Message content must not be blank")
    private String content;

    /**
     * AI model name (qwen | deepseek)
     */
    @Schema(description = "AI model name: qwen (Qwen) | deepseek (DeepSeek)")
    private String model;

    /**
     * System prompt
     */
    @Schema(description = "System prompt")
    private String systemPrompt;

    /**
     * History messages
     */
    @Schema(description = "History messages")
    private List<MessageDTO> history;

    /**
     * Streaming response flag
     */
    @Schema(description = "Whether to stream the response")
    @Builder.Default
    private Boolean stream = false;

    /**
     * Maximum token count
     */
    @Schema(description = "Maximum token count")
    private Integer maxTokens;

    /**
     * Temperature parameter
     */
    @Schema(description = "Temperature parameter")
    private Double temperature;

    /**
     * Whether knowledge base retrieval augmentation (RAG) is enabled
     */
    @Schema(description = "Whether knowledge base retrieval augmentation is enabled")
    @Builder.Default
    private boolean enableRag = false;

    /**
     * Whether knowledge graph augmentation (KAG) is enabled
     */
    @Schema(description = "Whether knowledge graph augmentation is enabled")
    @Builder.Default
    private boolean enableKAG = false;

    /**
     * Message DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Message information")
    public static class MessageDTO {

        /**
         * Role type
         */
        @Schema(description = "Role type")
        private String role;

        /**
         * Message content
         */
        @Schema(description = "Message content")
        private String content;
    }
}
