package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Notification VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification VO")
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Notification ID")
    private Long id;

    @Schema(description = "Recipient user ID")
    private Long userId;

    @Schema(description = "User name")
    private String userName;

    @Schema(description = "Notification type")
    private String notificationType;

    @Schema(description = "Notification title")
    private String title;

    @Schema(description = "Notification content")
    private String content;

    @Schema(description = "Redirect link")
    private String link;

    @Schema(description = "Related type")
    private String relatedType;

    @Schema(description = "Related ID")
    private Long relatedId;

    @Schema(description = "Whether read")
    private Integer isRead;

    @Schema(description = "Read time")
    private LocalDateTime readTime;

    @Schema(description = "Created time")
    private LocalDateTime createdAt;
}
