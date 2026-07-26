package com.knowledge.base.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Document review event DTO
 *
 * <p>Carries review events between kb-document (review logic) and kb-foundation (notification delivery)
 * via RabbitMQ, decoupling the modules and avoiding a direct dependency.</p>
 *
 * <p>Event types (eventType):
 * <ul>
 *   <li>SUBMITTED — document submitted for review, pushed to all reviewers</li>
 *   <li>APPROVED  — review approved, pushed to the document author</li>
 *   <li>REJECTED  — review rejected, pushed to the document author</li>
 * </ul>
 * </p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Event type: SUBMITTED / APPROVED / REJECTED */
    private String eventType;

    /** Document ID */
    private Long documentId;

    /** Document title */
    private String documentTitle;

    /** Document author ID */
    private Long authorId;

    /** Document author name (denormalized field, to avoid a lookup) */
    private String authorName;

    /** Reviewer ID */
    private Long reviewerId;

    /** Reviewer name */
    private String reviewerName;

    /** Review round */
    private Integer reviewRound;

    /** Review level (reserved for future multi-level support, default 1) */
    private Integer reviewLevel;

    /** Review comment (required when rejected) */
    private String reviewComment;

    /** Event timestamp */
    private LocalDateTime timestamp;
}
