package com.knowledge.base.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI feedback DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI feedback parameters")
public class FeedbackDTO {

    /**
     * Conversation ID
     */
    @Schema(description = "Conversation ID")
    @NotNull(message = "Conversation ID must not be null")
    private Long conversationId;

    /**
     * Message ID
     */
    @Schema(description = "Message ID")
    @NotNull(message = "Message ID must not be null")
    private Long messageId;

    /**
     * Feedback type (positive/negative)
     */
    @Schema(description = "Feedback type")
    @NotNull(message = "Feedback type must not be null")
    private String feedbackType;

    /**
     * Feedback content
     */
    @Schema(description = "Feedback content")
    private String feedbackContent;

    /**
     * Rating (1-5)
     */
    @Schema(description = "Rating")
    private Integer rating;
}
