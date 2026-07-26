package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * System notification entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_notification")
@Schema(description = "System notification entity")
public class Notification extends BaseEntity {

    @Schema(description = "Recipient user ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "User name (redundant field)")
    @TableField("user_name")
    private String userName;

    @Schema(description = "Notification type: system/comment/mention/review/like")
    @TableField("notification_type")
    private String notificationType;

    @Schema(description = "Notification title")
    @TableField("title")
    private String title;

    @Schema(description = "Notification content")
    @TableField("content")
    private String content;

    @Schema(description = "Redirect link")
    @TableField("link")
    private String link;

    @Schema(description = "Related type")
    @TableField("related_type")
    private String relatedType;

    @Schema(description = "Related ID")
    @TableField("related_id")
    private Long relatedId;

    @Schema(description = "Whether read: 0-unread, 1-read")
    @TableField("is_read")
    private Integer isRead;

    @Schema(description = "Read time")
    @TableField("read_time")
    private LocalDateTime readTime;

}
