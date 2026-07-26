package com.knowledge.base.foundation.mq;

import com.knowledge.base.common.config.SystemConfigCache;
import com.knowledge.base.common.event.ReviewEventDTO;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.dto.NotificationDTO;
import com.knowledge.base.foundation.feign.UserAuthFeignClient;
import com.knowledge.base.foundation.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Review notification message listener
 *
 * <p>Consumes review events from RabbitMQ and does two things:
 * <ol>
 *   <li>Persists the notification to the database (so offline users can see it too)</li>
 *   <li>Pushes it to online users in real time via WebSocket</li>
 * </ol>
 * </p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class ReviewNotificationListener {

    @Resource
    private NotificationService notificationService;

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Resource
    private UserAuthFeignClient userAuthFeignClient;

    @Resource
    private SystemConfigCache systemConfigCache;

    @RabbitListener(queues = "#{@reviewNotificationQueue.name}")
    public void handleReviewEvent(ReviewEventDTO event) {
        if (event == null || event.getEventType() == null) {
            log.warn("Received an empty review event, ignoring");
            return;
        }

        String notificationType = "review";
        String link = "/review/documents/" + event.getDocumentId();

        try {
            switch (event.getEventType()) {
                case "SUBMITTED" -> handleSubmitted(event, notificationType, link);
                case "APPROVED"  -> handleApproved(event, notificationType, link);
                case "REJECTED"  -> handleRejected(event, notificationType, link);
                default -> log.warn("Unknown review event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            // Catch all exceptions to avoid an infinite retry loop
            log.error("Failed to process review event: eventType={}, documentId={}, error={}",
                    event.getEventType(), event.getDocumentId(), e.getMessage(), e);
        }
    }

    /**
     * Handle a document-submitted-for-review event → notify reviewers, falling back to
     * notifying the author if no reviewer can be found
     */
    private void handleSubmitted(ReviewEventDTO event, String type, String link) {
        log.info("Received a document-submitted-for-review event: documentId={}, title={}, authorId={}",
                event.getDocumentId(), event.getDocumentTitle(), event.getAuthorId());

        // 1. Query reviewers
        List<Long> reviewerIds;
        try {
            reviewerIds = findReviewerUserIds();
        } catch (Exception e) {
            log.error("Failed to query reviewers: documentId={}, error={}", event.getDocumentId(), e.getMessage());
            reviewerIds = List.of();
        }

        if (reviewerIds.isEmpty()) {
            // Fallback: notify the author when no reviewer is found
            log.warn("No reviewer found; notifying the author instead: documentId={}, authorId={}",
                    event.getDocumentId(), event.getAuthorId());
            if (event.getAuthorId() != null) {
                String fallbackTitle = "Document submitted for review";
                String fallbackContent = "Your document \"" + event.getDocumentTitle() + "\" has been submitted for review. Please wait for a reviewer to process it.";
                persistAndPush(event.getAuthorId(), type, fallbackTitle, fallbackContent, link, event.getDocumentId());
            } else {
                log.warn("Submission event is missing an author ID; cannot send fallback notification: documentId={}", event.getDocumentId());
            }
            return;
        }

        // 2. Persist the notification + push point-to-point via WebSocket to each reviewer
        String title = "New document awaiting review";
        String content = String.format("User \"%s\" submitted the document \"%s\". Please review it promptly.",
                event.getAuthorName(), event.getDocumentTitle());
        for (Long reviewerId : reviewerIds) {
            persistAndPush(reviewerId, type, title, content, link, event.getDocumentId());
        }
    }

    /**
     * Handle a review-approved event → notify the document author
     */
    private void handleApproved(ReviewEventDTO event, String type, String link) {
        String title = "Document approved";
        String content = String.format("Your document \"%s\" has passed review and is now published.",
                event.getDocumentTitle());

        if (event.getAuthorId() != null) {
            persistAndPush(event.getAuthorId(), type, title, content, link, event.getDocumentId());
        } else {
            log.warn("Approval event is missing an author ID; skipping notification push: documentId={}", event.getDocumentId());
        }
    }

    /**
     * Handle a review-rejected event → notify the document author
     */
    private void handleRejected(ReviewEventDTO event, String type, String link) {
        String title = "Document review rejected";
        String reason = event.getReviewComment() != null && !event.getReviewComment().isBlank()
                ? event.getReviewComment() : "No reason provided";
        String content = String.format("Your document \"%s\" did not pass review. Reason for rejection: %s",
                event.getDocumentTitle(), reason);

        if (event.getAuthorId() != null) {
            persistAndPush(event.getAuthorId(), type, title, content, link, event.getDocumentId());
        } else {
            log.warn("Rejection event is missing an author ID; skipping notification push: documentId={}", event.getDocumentId());
        }
    }

    /**
     * Persist the notification to the database + push point-to-point via WebSocket (if enabled)
     */
    private void persistAndPush(Long userId, String type, String title,
                                 String content, String link, Long documentId) {
        // 1. Persist to the database (always executed)
        NotificationDTO dto = new NotificationDTO();
        dto.setUserId(userId);
        dto.setNotificationType(type);
        dto.setTitle(title);
        dto.setContent(content);
        dto.setLink(link);
        dto.setRelatedType("document");
        dto.setRelatedId(documentId);
        dto.setIsRead(0);
        try {
            notificationService.sendNotification(dto);
            log.info("Notification persisted: userId={}, type={}, documentId={}", userId, type, documentId);
        } catch (Exception e) {
            log.error("Failed to persist notification: userId={}, type={}, documentId={}, error={}",
                    userId, type, documentId, e.getMessage(), e);
            // A persistence failure does not affect the WebSocket push (real-time notification is still attempted)
        }

        // 2. Push point-to-point to the target user via WebSocket (only when WebSocket is enabled)
        if (!isWebSocketEnabled()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("notificationType", type);
        payload.put("title", title);
        payload.put("content", content);
        payload.put("link", link);
        payload.put("documentId", documentId);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId), "/queue/notifications", payload);
    }

    /**
     * Check whether WebSocket push is enabled
     */
    private boolean isWebSocketEnabled() {
        String value = systemConfigCache.getConfig("websocket.enabled");
        return value == null || "true".equals(value);
    }

    /**
     * Query all possible recipient IDs for review notifications
     *
     * <p>Queried in priority order: ROLE_REVIEWER → ROLE_ADMIN → ROLE_SUPER_ADMIN</p>
     */
    private List<Long> findReviewerUserIds() {
        try {
            // 1. Prefer querying reviewers
            Result<List<Long>> result = userAuthFeignClient.getUserIdsByRole("ROLE_REVIEWER");
            if (result != null && result.getData() != null && !result.getData().isEmpty()) {
                List<Long> ids = result.getData();
                log.info("Found {} reviewer(s): ids={}", ids.size(), ids);
                return ids;
            }

            // 2. Fallback: query the ROLE_ADMIN role
            log.warn("No reviewer found; falling back to the admin role");
            Result<List<Long>> adminResult = userAuthFeignClient.getUserIdsByRole("ROLE_ADMIN");
            if (adminResult != null && adminResult.getData() != null && !adminResult.getData().isEmpty()) {
                List<Long> adminIds = adminResult.getData();
                log.info("Using {} admin(s) as review notification recipients: ids={}", adminIds.size(), adminIds);
                return adminIds;
            }

            // 3. Final fallback: query ROLE_SUPER_ADMIN (the default role for the admin account)
            log.warn("No reviewer or admin found; falling back to the super admin role");
            Result<List<Long>> superAdminResult = userAuthFeignClient.getUserIdsByRole("ROLE_SUPER_ADMIN");
            if (superAdminResult != null && superAdminResult.getData() != null && !superAdminResult.getData().isEmpty()) {
                List<Long> superAdminIds = superAdminResult.getData();
                log.info("Using {} super admin(s) as review notification recipients: ids={}", superAdminIds.size(), superAdminIds);
                return superAdminIds;
            }

            log.warn("No review notification recipients found (REVIEWER/ADMIN/SUPER_ADMIN are all empty)");
            return List.of();
        } catch (Exception e) {
            log.error("Feign query for reviewers failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Build the WebSocket broadcast push payload
     */
    private Map<String, Object> buildPushPayload(ReviewEventDTO event,
            String type, String title, String content, String link) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", event.getEventType());
        payload.put("notificationType", type);
        payload.put("title", title);
        payload.put("content", content);
        payload.put("link", link);
        payload.put("documentId", event.getDocumentId());
        payload.put("documentTitle", event.getDocumentTitle());
        payload.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : "");
        return payload;
    }
}
