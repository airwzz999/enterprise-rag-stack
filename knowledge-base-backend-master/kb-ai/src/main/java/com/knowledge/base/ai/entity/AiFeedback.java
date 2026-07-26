package com.knowledge.base.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI feedback entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_feedback")
public class AiFeedback {

    /**
     * Primary key ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Conversation ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * Message ID
     */
    @TableField("message_id")
    private Long messageId;

    /**
     * User ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Feedback type (positive/negative)
     */
    @TableField("feedback_type")
    private String feedbackType;

    /**
     * Feedback content
     */
    @TableField("feedback_content")
    private String feedbackContent;

    /**
     * Rating (1-5)
     */
    @TableField("rating")
    private Integer rating;

    /**
     * Creation time
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Logical deletion flag
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
