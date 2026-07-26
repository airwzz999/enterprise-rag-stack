package com.knowledge.base.document.event;

import com.knowledge.base.common.config.InstanceIdentifier;
import com.knowledge.base.common.event.StatisticsEventDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Statistics event publisher
 *
 * <p>Responsible for asynchronously publishing business operation events to RabbitMQ, for consumption by the statistics service</p>
 * <p>The routing key uses InstanceIdentifier for instance isolation, ensuring events produced by this instance are only consumed by this instance's statistics service</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class StatisticsEventPublisher {

    /** Statistics exchange (shared by all instances) */
    private static final String STATISTICS_EXCHANGE = "kb.statistics.exchange";

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    @Resource
    private InstanceIdentifier instanceIdentifier;

    private String viewRoutingKey() {
        return "statistics.view." + instanceIdentifier.getId() + ".record";
    }

    private String likeRoutingKey() {
        return "statistics.like." + instanceIdentifier.getId() + ".record";
    }

    private String commentRoutingKey() {
        return "statistics.comment." + instanceIdentifier.getId() + ".record";
    }

    /**
     * Publishes a document view event (asynchronous, does not block the main flow)
     */
    public void publishViewEvent(Long userId, String userName, Long documentId, String documentTitle) {
        CompletableFuture.runAsync(() -> {
            try {
                StatisticsEventDTO event = buildEvent("VIEW", userId, userName, documentId, documentTitle);
                rabbitTemplate.convertAndSend(STATISTICS_EXCHANGE, viewRoutingKey(), event);
                log.debug("View event published successfully: documentId={}", documentId);
            } catch (Exception e) {
                log.error("Failed to publish view event: documentId={}, error={}", documentId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Publishes a document like event (asynchronous)
     */
    public void publishLikeEvent(Long userId, String userName, Long documentId, String documentTitle) {
        CompletableFuture.runAsync(() -> {
            try {
                StatisticsEventDTO event = buildEvent("LIKE", userId, userName, documentId, documentTitle);
                rabbitTemplate.convertAndSend(STATISTICS_EXCHANGE, likeRoutingKey(), event);
                log.debug("Like event published successfully: documentId={}", documentId);
            } catch (Exception e) {
                log.error("Failed to publish like event: documentId={}, error={}", documentId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Publishes a comment-created event (asynchronous)
     */
    public void publishCommentEvent(Long userId, String userName, Long documentId, String documentTitle) {
        CompletableFuture.runAsync(() -> {
            try {
                StatisticsEventDTO event = buildEvent("COMMENT", userId, userName, documentId, documentTitle);
                rabbitTemplate.convertAndSend(STATISTICS_EXCHANGE, commentRoutingKey(), event);
                log.debug("Comment event published successfully: documentId={}", documentId);
            } catch (Exception e) {
                log.error("Failed to publish comment event: documentId={}, error={}", documentId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    private StatisticsEventDTO buildEvent(String eventType, Long userId, String userName,
                                          Long documentId, String documentTitle) {
        return StatisticsEventDTO.builder()
                .eventType(eventType)
                .userId(userId)
                .userName(userName)
                .documentId(documentId)
                .documentTitle(documentTitle)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
