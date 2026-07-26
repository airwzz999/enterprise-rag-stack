package com.knowledge.base.statistics.task;

import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Statistics aggregation scheduled task
 *
 * <p>Daily scheduled job that aggregates raw kb_view_history data into the pre-aggregated tables,
 * reducing the computational overhead of real-time queries and improving statistics API response times.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class StatisticsAggregationTask {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_VIEW_COUNTER_PREFIX = "stats:counter:view:";
    private static final String REDIS_LIKE_COUNTER_PREFIX = "stats:counter:like:";
    private static final String REDIS_COMMENT_COUNTER_PREFIX = "stats:counter:comment:";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Daily document statistics aggregation
     *
     * <p>Runs daily at 00:05, aggregating the previous day's view/like/comment data into kb_document_statistics</p>
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void aggregateDailyDocumentStatistics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateStr = yesterday.format(DATE_FMT);
        log.info("Starting daily document statistics aggregation: date={}", dateStr);

        try {
            // Query the previous day's view data, aggregated by document
            String querySql = "SELECT document_id, MAX(document_title) AS document_title, COUNT(*) AS view_count " +
                    "FROM kb_view_history " +
                    "WHERE DATE(created_at) = ? " +
                    "GROUP BY document_id";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(querySql, dateStr);

            if (rows.isEmpty()) {
                log.info("No document view data to aggregate: date={}", dateStr);
                return;
            }

            // Batch INSERT ... ON DUPLICATE KEY UPDATE
            String insertSql = "INSERT INTO kb_document_statistics " +
                    "(id, document_id, document_title, view_count, like_count, comment_count, stat_date, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW()) " +
                    "ON DUPLICATE KEY UPDATE view_count = VALUES(view_count), " +
                    "like_count = VALUES(like_count), comment_count = VALUES(comment_count)";

            List<Object[]> batchArgs = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Long documentId = toLong(row.get("document_id"));
                String documentTitle = (String) row.get("document_title");
                long viewCount = toLong(row.get("view_count"));

                // Reads like and comment counts from Redis
                long likeCount = getRedisCounter(REDIS_LIKE_COUNTER_PREFIX, documentId, yesterday);
                long commentCount = getRedisCounter(REDIS_COMMENT_COUNTER_PREFIX, documentId, yesterday);

                batchArgs.add(new Object[]{
                        SnowflakeIdGenerator.getInstance().nextId(),
                        documentId,
                        documentTitle,
                        viewCount,
                        likeCount,
                        commentCount,
                        dateStr
                });
            }

            jdbcTemplate.batchUpdate(insertSql, batchArgs);
            log.info("Daily document statistics aggregation complete: date={}, documentCount={}", dateStr, rows.size());
        } catch (Exception e) {
            log.error("Daily document statistics aggregation failed: date={}, error={}", dateStr, e.getMessage(), e);
        }
    }

    /**
     * Daily user statistics aggregation
     *
     * <p>Runs daily at 00:10, aggregating the previous day's user view data into kb_user_statistics</p>
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void aggregateDailyUserStatistics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateStr = yesterday.format(DATE_FMT);
        log.info("Starting daily user statistics aggregation: date={}", dateStr);

        try {
            // Query the previous day's view data, aggregated by user
            String querySql = "SELECT user_id, MAX(user_name) AS user_name, COUNT(*) AS view_count " +
                    "FROM kb_view_history " +
                    "WHERE DATE(created_at) = ? AND user_id IS NOT NULL " +
                    "GROUP BY user_id";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(querySql, dateStr);

            if (rows.isEmpty()) {
                log.info("No user view data to aggregate: date={}", dateStr);
                return;
            }

            // Batch INSERT ... ON DUPLICATE KEY UPDATE
            String insertSql = "INSERT INTO kb_user_statistics " +
                    "(id, user_id, user_name, document_count, comment_count, like_count, view_count, login_count, stat_date, created_at) " +
                    "VALUES (?, ?, ?, 0, 0, 0, ?, 0, ?, NOW()) " +
                    "ON DUPLICATE KEY UPDATE view_count = VALUES(view_count)";

            List<Object[]> batchArgs = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Long userId = toLong(row.get("user_id"));
                String userName = (String) row.get("user_name");
                long viewCount = toLong(row.get("view_count"));

                batchArgs.add(new Object[]{
                        SnowflakeIdGenerator.getInstance().nextId(),
                        userId,
                        userName,
                        viewCount,
                        dateStr
                });
            }

            jdbcTemplate.batchUpdate(insertSql, batchArgs);
            log.info("Daily user statistics aggregation complete: date={}, userCount={}", dateStr, rows.size());
        } catch (Exception e) {
            log.error("Daily user statistics aggregation failed: date={}, error={}", dateStr, e.getMessage(), e);
        }
    }

    /**
     * Cleans up expired view history records
     *
     * <p>Runs daily at 02:30, deleting view history records older than 90 days to keep table size under control</p>
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void cleanupOldViewHistory() {
        log.info("Starting cleanup of expired view history records (retaining 90 days)");

        try {
            int deleted = jdbcTemplate.update(
                    "DELETE FROM kb_view_history WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY)");
            log.info("Expired view history cleanup complete: deleted {} records", deleted);
        } catch (Exception e) {
            log.error("Failed to clean up expired view history records: error={}", e.getMessage(), e);
        }
    }

    /**
     * Reads a counter value from Redis
     *
     * @param prefix      the Redis key prefix
     * @param documentId  the document ID
     * @param date        the date
     * @return the count value; returns 0 if the read fails
     */
    private long getRedisCounter(String prefix, Long documentId, LocalDate date) {
        try {
            String key = prefix + documentId + ":" + date.format(DATE_FMT);
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        } catch (Exception e) {
            log.debug("Failed to read Redis counter: prefix={}, docId={}, date={}", prefix, documentId, date);
        }
        return 0;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
