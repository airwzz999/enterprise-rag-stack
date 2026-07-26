package com.knowledge.base.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Conversation VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Conversation information")
public class ConversationVO {

    /**
     * Primary key ID
     */
    @Schema(description = "Primary key ID")
    private Long id;

    /**
     * Conversation title
     */
    @Schema(description = "Conversation title")
    private String title;

    /**
     * Model name
     */
    @Schema(description = "Model name")
    private String model;

    /**
     * Token usage
     */
    @Schema(description = "Token usage")
    private Integer tokensUsed;

    /**
     * Message count
     */
    @Schema(description = "Message count")
    private Integer messageCount;

    /**
     * Status
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    @Schema(description = "Update time")
    private LocalDateTime updatedAt;

    /**
     * List of messages
     */
    @Schema(description = "List of messages")
    private List<MessageVO> messages;
}
