package com.knowledge.base.statistics.mq;

import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.common.event.StatisticsEventDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Statistics event MQ listener
 *
 * <p>Consumes statistics events from business services, writing to the kb_view_history table and Redis counters</p>
 * <p>Queue names dynamically reference instance-isolated Queue beans via SpEL expressions</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class StatisticsMQListener {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_VIEW_COUNTER_PREFIX = "stats:counter:view:";
    private static final String REDIS_LIKE_COUNTER_PREFIX = "stats:counter:like:";
    private static final String REDIS_COMMENT_COUNTER_PREFIX = "stats:counter:comment:";

    /**
     * Consumes view events
     */
    @RabbitListener(queues = "#{@statisticsViewQueue.name}")
    public void handleViewEvent(StatisticsEventDTO event) {
        try {
            log.debug("Received view event: userId={}, documentId={}, title={}",
                    event.getUserId(), event.getDocumentId(), event.getDocumentTitle());

            // Write to the view history table
            long recordId = SnowflakeIdGenerator.getInstance().nextId();
            jdbcTemplate.update(
                    "INSERT INTO kb_view_history (id, user_id, user_name, document_id, document_title, " +
                            "ip_address, user_agent, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                    recordId,
                    event.getUserId(),
                    event.getUserName(),
                    event.getDocumentId(),
                    event.getDocumentTitle(),
                    event.getIpAddress(),
                    event.getUserAgent()
            );

            // Update the real-time counter in Redis
            String todayKey = REDIS_VIEW_COUNTER_PREFIX + event.getDocumentId() + ":" + LocalDate.now();
            redisTemplate.opsForValue().increment(todayKey);
            redisTemplate.expire(todayKey, 48, TimeUnit.HOURS);

            log.debug("View event processing complete: recordId={}", recordId);
        } catch (Exception e) {
            log.error("Failed to process view event: userId={}, documentId={}, error={}",
                    event.getUserId(), event.getDocumentId(), e.getMessage(), e);
            // Does not throw, to avoid infinite message retries
        }
    }

    /**
     * Consumes like events
     */
    @RabbitListener(queues = "#{@statisticsLikeQueue.name}")
    public void handleLikeEvent(StatisticsEventDTO event) {
        try {
            log.debug("Received like event: userId={}, documentId={}", event.getUserId(), event.getDocumentId());

            // Update the real-time like counter in Redis
            String todayKey = REDIS_LIKE_COUNTER_PREFIX + event.getDocumentId() + ":" + LocalDate.now();
            redisTemplate.opsForValue().increment(todayKey);
            redisTemplate.expire(todayKey, 48, TimeUnit.HOURS);

            log.debug("Like event processing complete: documentId={}", event.getDocumentId());
        } catch (Exception e) {
            log.error("Failed to process like event: documentId={}, error={}",
                    event.getDocumentId(), e.getMessage(), e);
        }
    }

    /**
     * Consumes comment events
     */
    @RabbitListener(queues = "#{@statisticsCommentQueue.name}")
    public void handleCommentEvent(StatisticsEventDTO event) {
        try {
            log.debug("Received comment event: userId={}, documentId={}", event.getUserId(), event.getDocumentId());

            // Update the real-time comment counter in Redis
            String todayKey = REDIS_COMMENT_COUNTER_PREFIX + event.getDocumentId() + ":" + LocalDate.now();
            redisTemplate.opsForValue().increment(todayKey);
            redisTemplate.expire(todayKey, 48, TimeUnit.HOURS);

            log.debug("Comment event processing complete: documentId={}", event.getDocumentId());
        } catch (Exception e) {
            log.error("Failed to process comment event: documentId={}, error={}",
                    event.getDocumentId(), e.getMessage(), e);
        }
    }
}
