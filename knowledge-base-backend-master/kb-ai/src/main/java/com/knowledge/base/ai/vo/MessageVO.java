package com.knowledge.base.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI message VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI message information")
public class MessageVO {

    /**
     * Message ID
     */
    @Schema(description = "Message ID")
    private Long id;

    /**
     * Conversation ID
     */
    @Schema(description = "Conversation ID")
    private Long conversationId;

    /**
     * Role type (system/user/assistant)
     */
    @Schema(description = "Role type")
    private String role;

    /**
     * Message content
     */
    @Schema(description = "Message content")
    private String content;

    /**
     * Token count
     */
    @Schema(description = "Token count")
    private Integer tokens;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
