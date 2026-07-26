package com.knowledge.base.statistics.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.statistics.entity.DocumentStatistics;
import com.knowledge.base.statistics.entity.UserStatistics;
import com.knowledge.base.statistics.mapper.DocumentStatisticsMapper;
import com.knowledge.base.statistics.mapper.UserStatisticsMapper;
import com.knowledge.base.statistics.vo.HotDocumentVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Scheduled task to refresh the latest-documents cache
 *
 * <p>Every 5 minutes, queries the top 6 most recently published documents,
 * and writes to the Redis + Caffeine local cache.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class LatestDocumentsCacheTask {

    @Resource
    private DocumentStatisticsMapper documentMapper;

    @Resource
    private UserStatisticsMapper userMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    private static final String REDIS_KEY = "stats:latestDocuments:top6";
    private static final String CAFFEINE_CACHE_NAME = "latestDocuments";
    private static final String CAFFEINE_CACHE_KEY = "top6";
    private static final int TOP_N = 6;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Runs a refresh immediately once after the service starts
     */
    @PostConstruct
    public void init() {
        log.info("LatestDocumentsCacheTask initialized, starting the initial latest-documents cache refresh...");
        refreshLatestDocuments();
    }

    /**
     * Refreshes the latest-documents cache every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void refreshLatestDocuments() {
        log.info("Starting to refresh the latest-documents cache...");
        try {
            List<HotDocumentVO> latestDocs = computeLatestDocuments();

            if (latestDocs == null || latestDocs.isEmpty()) {
                log.warn("No documents found, skipping cache refresh");
                return;
            }

            // Write to Redis (L2)
            redisTemplate.opsForValue().set(REDIS_KEY, latestDocs);
            log.debug("Latest documents written to Redis: key={}, count={}", REDIS_KEY, latestDocs.size());

            // Write to the Caffeine local cache (L1)
            org.springframework.cache.Cache cache = caffeineCacheManager.getCache(CAFFEINE_CACHE_NAME);
            if (cache != null) {
                cache.put(CAFFEINE_CACHE_KEY, latestDocs);
                log.debug("Latest documents written to Caffeine: cache={}, key={}", CAFFEINE_CACHE_NAME, CAFFEINE_CACHE_KEY);
            } else {
                log.warn("Caffeine cache '{}' does not exist, please check the CacheConfig configuration", CAFFEINE_CACHE_NAME);
            }

            log.info("Latest-documents cache refresh complete: {} entries", latestDocs.size());
        } catch (Exception e) {
            log.error("Failed to refresh the latest-documents cache", e);
        }
    }

    /**
     * Queries the latest top N documents (ordered by creation time descending)
     */
    List<HotDocumentVO> computeLatestDocuments() {
        List<DocumentStatistics> latestDocs = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentStatistics>()
                        .eq(DocumentStatistics::getStatus, 1)
                        .eq(DocumentStatistics::getDeleted, 0)
                        .orderByDesc(DocumentStatistics::getCreatedAt)
                        .last("LIMIT " + TOP_N));

        if (latestDocs == null || latestDocs.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch-fetch author names
        Map<Long, String> authorNameMap = buildAuthorNameMap(latestDocs);

        // Batch-fetch category names
        Map<Long, String> categoryNameMap = buildCategoryNameMap(latestDocs);

        // Batch-fetch document summaries
        Map<Long, String> summaryMap = buildSummaryMap(latestDocs);

        return latestDocs.stream()
                .map(doc -> HotDocumentVO.builder()
                        .documentId(doc.getId())
                        .title(doc.getTitle())
                        .authorId(doc.getAuthorId())
                        .authorName(authorNameMap.getOrDefault(doc.getAuthorId(), ""))
                        .categoryId(doc.getCategoryId())
                        .categoryName(categoryNameMap.getOrDefault(doc.getCategoryId(), ""))
                        .viewCount(nullSafe(doc.getViewCount()))
                        .likeCount(nullSafe(doc.getLikeCount()))
                        .favoriteCount(nullSafe(doc.getFavoriteCount()))
                        .commentCount(0L)
                        .summary(summaryMap.getOrDefault(doc.getId(), ""))
                        .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().format(DATE_FMT) : "")
                        .statisticsValue(0L)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Batch-builds an author ID → username mapping
     */
    private Map<Long, String> buildAuthorNameMap(List<DocumentStatistics> docs) {
        List<Long> authorIds = docs.stream()
                .map(DocumentStatistics::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UserStatistics> users = userMapper.selectBatchIds(authorIds);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }

        return users.stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(
                        UserStatistics::getId,
                        u -> {
                            String realName = u.getRealName();
                            return (realName != null && !realName.isEmpty()) ? realName : u.getUsername();
                        },
                        (a, b) -> a));
    }

    /**
     * Batch-builds a category ID → category name mapping
     */
    private Map<Long, String> buildCategoryNameMap(List<DocumentStatistics> docs) {
        List<Long> categoryIds = docs.stream()
                .map(DocumentStatistics::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> nameMap = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, category_name FROM kb_category WHERE deleted = 0");
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Long id = toLong(row.get("id"));
                    String name = (String) row.get("category_name");
                    if (id != null && categoryIds.contains(id)) {
                        nameMap.put(id, name != null ? name : "Unnamed");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to build category name mapping: {}", e.getMessage());
        }
        return nameMap;
    }

    /**
     * Batch-builds a document ID → summary mapping
     */
    private Map<Long, String> buildSummaryMap(List<DocumentStatistics> docs) {
        List<Long> docIds = docs.stream()
                .map(DocumentStatistics::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (docIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> summaryMap = new HashMap<>();
        try {
            String placeholders = docIds.stream().map(id -> "?").collect(Collectors.joining(","));
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, summary FROM kb_document WHERE deleted = 0 AND id IN (" + placeholders + ")",
                    docIds.toArray());
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Long id = toLong(row.get("id"));
                    String summary = (String) row.get("summary");
                    if (id != null) {
                        summaryMap.put(id, summary != null ? summary : "");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to build document summary mapping: {}", e.getMessage());
        }
        return summaryMap;
    }

    private long nullSafe(Long value) {
        return value != null ? value : 0L;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
