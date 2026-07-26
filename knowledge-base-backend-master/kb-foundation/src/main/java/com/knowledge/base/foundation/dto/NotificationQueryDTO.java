package com.knowledge.base.foundation.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Notification query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Notification query DTO")
public class NotificationQueryDTO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Notification type")
    private String notificationType;

    @Schema(description = "Whether read: 0-unread, 1-read")
    private Integer isRead;

    @Schema(description = "Start time")
    private String startTime;

    @Schema(description = "End time")
    private String endTime;
}
