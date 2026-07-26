package com.knowledge.base.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI conversation entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("conversation")
public class Conversation {

    /**
     * Primary key ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Conversation title
     */
    @TableField("title")
    private String title;

    /**
     * User ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Model name
     */
    @TableField("model")
    private String model;

    /**
     * System prompt
     */
    @TableField("system_prompt")
    private String systemPrompt;

    /**
     * Token usage
     */
    @TableField("tokens_used")
    private Integer tokensUsed;

    /**
     * Message count
     */
    @TableField("message_count")
    private Integer messageCount;

    /**
     * Status (0 - in progress, 1 - ended)
     */
    @TableField("status")
    private Integer status;

    /**
     * Creation time
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * Logical deletion flag
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
