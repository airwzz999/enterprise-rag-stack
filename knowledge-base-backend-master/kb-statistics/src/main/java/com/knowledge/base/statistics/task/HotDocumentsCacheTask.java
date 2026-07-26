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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Scheduled task to refresh the popular-documents cache
 *
 * <p>Every 30 minutes, computes a composite popularity score from each document's view count, like count, and favorite count,
 * Takes the top 6 documents and writes them to the Redis + Caffeine local cache.</p>
 *
 * <p>Popularity score formula: score = viewCount * 1 + likeCount * 3 + favoriteCount * 5</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class HotDocumentsCacheTask {

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

    private static final String REDIS_KEY = "stats:hotDocuments:top6";
    private static final String CAFFEINE_CACHE_NAME = "hotDocuments";
    private static final String CAFFEINE_CACHE_KEY = "top6";
    private static final int TOP_N = 6;

    /** View count weight */
    private static final int SCORE_VIEW = 1;
    /** Like weight */
    private static final int SCORE_LIKE = 3;
    /** Favorite weight */
    private static final int SCORE_FAVORITE = 5;

    /**
     * Runs a refresh immediately once after the service starts
     */
    @PostConstruct
    public void init() {
        log.info("HotDocumentsCacheTask initialized, starting the initial popular-documents cache refresh...");
        refreshHotDocuments();
    }

    /**
     * Refreshes the popular-documents cache every 30 minutes
     */
    @Scheduled(fixedRate = 1800000)
    public void refreshHotDocuments() {
        log.info("Starting to refresh the popular-documents cache...");
        try {
            List<HotDocumentVO> topDocs = computeTopDocuments();

            if (topDocs == null || topDocs.isEmpty()) {
                log.warn("No documents found, skipping cache refresh");
                return;
            }

            // Write to Redis (L2)
            redisTemplate.opsForValue().set(REDIS_KEY, topDocs);
            log.debug("Popular documents written to Redis: key={}, count={}", REDIS_KEY, topDocs.size());

            // Write to the Caffeine local cache (L1)
            org.springframework.cache.Cache cache = caffeineCacheManager.getCache(CAFFEINE_CACHE_NAME);
            if (cache != null) {
                cache.put(CAFFEINE_CACHE_KEY, topDocs);
                log.debug("Popular documents written to Caffeine: cache={}, key={}", CAFFEINE_CACHE_NAME, CAFFEINE_CACHE_KEY);
            } else {
                log.warn("Caffeine cache '{}' does not exist, please check the CacheConfig configuration", CAFFEINE_CACHE_NAME);
            }

            log.info("Popular-documents cache refresh complete: {} entries", topDocs.size());
        } catch (Exception e) {
            log.error("Failed to refresh the popular-documents cache", e);
        }
    }

    /**
     * Queries and computes the top N popular documents
     *
     * <p>Uses a composite popularity score: score = viewCount * 1 + likeCount * 3 + favoriteCount * 5</p>
     */
    List<HotDocumentVO> computeTopDocuments() {
        // 1. Query all non-deleted documents (pre-filtered by view count to reduce computation)
        List<DocumentStatistics> allDocs = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentStatistics>()
                        .eq(DocumentStatistics::getStatus, 1)
                        .eq(DocumentStatistics::getDeleted, 0)
                        .gt(DocumentStatistics::getViewCount, 0)
                        .orderByDesc(DocumentStatistics::getViewCount)
                        .last("LIMIT 500"));

        if (allDocs == null || allDocs.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Sort by composite popularity score and take the top N
        List<DocumentStatistics> topDocs = allDocs.stream()
                .sorted((a, b) -> Long.compare(compositeScore(b), compositeScore(a)))
                .limit(TOP_N)
                .collect(Collectors.toList());

        // 3. Batch-fetch the author name mapping
        Map<Long, String> authorNameMap = buildAuthorNameMap(topDocs);

        // 4. Batch-fetch the category name mapping
        Map<Long, String> categoryNameMap = buildCategoryNameMap(topDocs);

        // 5. Batch-fetch the document summary mapping
        Map<Long, String> summaryMap = buildSummaryMap(topDocs);

        // 6. Assemble the VO list
        return topDocs.stream()
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
                        .statisticsValue(compositeScore(doc))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Computes a document's composite popularity score
     */
    private long compositeScore(DocumentStatistics doc) {
        return nullSafe(doc.getViewCount()) * SCORE_VIEW
                + nullSafe(doc.getLikeCount()) * SCORE_LIKE
                + nullSafe(doc.getFavoriteCount()) * SCORE_FAVORITE;
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
            // Build the IN-query placeholders
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
