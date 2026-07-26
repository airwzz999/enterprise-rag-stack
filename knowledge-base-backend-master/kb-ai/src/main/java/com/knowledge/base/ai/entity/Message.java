package com.knowledge.base.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI message entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message")
public class Message {

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
     * Role type (system/user/assistant)
     */
    @TableField("role")
    private String role;

    /**
     * Message content
     */
    @TableField("content")
    private String content;

    /**
     * Token count
     */
    @TableField("tokens")
    private Integer tokens;

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
