package com.knowledge.base.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Statistics event DTO
 *
 * <p>Event message passed between business services and the statistics service via RabbitMQ</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Event type: VIEW / LIKE / FAVORITE / COMMENT / CREATE */
    private String eventType;

    /** User ID (null when not logged in) */
    private Long userId;

    /** Username */
    private String userName;

    /** Document ID */
    private Long documentId;

    /** Document title */
    private String documentTitle;

    /** IP address */
    private String ipAddress;

    /** User agent */
    private String userAgent;

    /** Event timestamp */
    private LocalDateTime timestamp;
}
