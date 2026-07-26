package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Comment entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_comment")
@Schema(description = "Comment entity")
public class Comment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Comment ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Comment ID")
    private Long id;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Parent comment ID
     */
    @Schema(description = "Parent comment ID")
    private Long parentId;

    /**
     * Root comment ID (not yet defined in the table)
     */
    @TableField(exist = false)
    @Schema(description = "Root comment ID")
    private Long rootId;

    /**
     * Comment content
     */
    @Schema(description = "Comment content")
    private String content;

    /**
     * Commenter ID
     */
    @TableField("user_id")
    @Schema(description = "Commenter ID")
    private Long commenterId;

    /**
     * Commenter name
     */
    @TableField("user_name")
    @Schema(description = "Commenter name")
    private String commenterName;

    /**
     * Commenter avatar
     */
    @TableField("user_avatar")
    @Schema(description = "Commenter avatar")
    private String commenterAvatar;

    /**
     * Reply-to user (user ID)
     */
    @TableField("reply_to_id")
    @Schema(description = "Reply-to user")
    private Long replyToUserId;

    /**
     * Reply-to user (user name)
     */
    @TableField("reply_to_name")
    @Schema(description = "Reply-to user")
    private String replyToUserName;

    /**
     * Status: 0-hidden, 1-normal
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Like count
     */
    @Schema(description = "Like count")
    private Integer likeCount;

    /**
     * Reply count
     */
    @Schema(description = "Reply count")
    private Integer replyCount;

    /**
     * Delete flag
     */
    @TableLogic
    @Schema(description = "Delete flag")
    private Integer deleted;
}
