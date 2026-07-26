package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Notification DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Notification DTO")
public class NotificationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Notification ID")
    private Long id;

    @NotNull(message = "Recipient user ID must not be empty")
    @Schema(description = "Recipient user ID")
    private Long userId;

    @Schema(description = "User name (redundant field)")
    private String userName;

    @NotBlank(message = "Notification type must not be empty")
    @Schema(description = "Notification type: system/comment/mention/review/like")
    private String notificationType;

    @NotBlank(message = "Notification title must not be empty")
    @Schema(description = "Notification title")
    private String title;

    @NotBlank(message = "Notification content must not be empty")
    @Schema(description = "Notification content")
    private String content;

    @Schema(description = "Redirect link")
    private String link;

    @Schema(description = "Related type")
    private String relatedType;

    @Schema(description = "Related ID")
    private Long relatedId;

    @Schema(description = "Whether read: 0-unread, 1-read")
    private Integer isRead;
}
