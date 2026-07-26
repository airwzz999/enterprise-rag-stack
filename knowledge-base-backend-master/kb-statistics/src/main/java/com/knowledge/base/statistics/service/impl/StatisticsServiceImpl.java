package com.knowledge.base.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.knowledge.base.statistics.entity.CommentStatistics;
import com.knowledge.base.statistics.entity.DocumentStatistics;
import com.knowledge.base.statistics.entity.UserStatistics;
import com.knowledge.base.statistics.entity.ViewStatistics;
import com.knowledge.base.statistics.mapper.AiStatisticsMapper;
import com.knowledge.base.statistics.mapper.CommentStatisticsMapper;
import com.knowledge.base.statistics.mapper.DocumentStatisticsMapper;
import com.knowledge.base.statistics.mapper.DocumentStatisticsAggMapper;
import com.knowledge.base.statistics.mapper.UserStatisticsMapper;
import com.knowledge.base.statistics.mapper.UserStatisticsAggMapper;
import com.knowledge.base.statistics.mapper.ViewStatisticsMapper;
import com.knowledge.base.statistics.model.DailyCount;
import com.knowledge.base.statistics.model.IdCount;
import com.knowledge.base.statistics.service.StatisticsService;
import com.knowledge.base.statistics.vo.ActiveUserVO;
import com.knowledge.base.statistics.vo.AdminOverviewVO;
import com.knowledge.base.statistics.vo.CategoryDistributionVO;
import com.knowledge.base.statistics.vo.DashboardVO;
import com.knowledge.base.statistics.vo.HotDocumentVO;
import com.knowledge.base.statistics.vo.OverviewVO;
import com.knowledge.base.statistics.vo.TrendVO;
import com.knowledge.base.statistics.vo.UserActivityVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Statistics analysis service implementation
 *
 * <p>Enterprise-grade implementation standards:
 * <ul>
 *   <li>Hot data is cached in Redis to reduce DB load</li>
 *   <li>Batch aggregate queries replace N+1 single-row queries</li>
 *   <li>All XML Mapper predefined query methods are fully wired up</li>
 *   <li>Cache penetration is prevented via Cacheable + an unless condition</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Resource
    private DocumentStatisticsMapper documentMapper;

    @Resource
    private UserStatisticsMapper userMapper;

    @Resource
    private CommentStatisticsMapper commentMapper;

    @Resource
    private ViewStatisticsMapper viewMapper;

    @Resource
    private DocumentStatisticsAggMapper documentStatisticsAggMapper;

    @Resource
    private UserStatisticsAggMapper userStatisticsAggMapper;

    @Resource
    private AiStatisticsMapper aiStatisticsMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    // ======================== Local in-memory cache ========================

    /** Homepage data overview cache: expires automatically after 10 minutes to reduce DB queries */
    private final LoadingCache<String, OverviewVO> overviewCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(1)
            .build(key -> loadOverview());

    // ======================== Cache key constants ========================
    private static final String CACHE_COMPOSITE_REDIS_KEY = "stats:hotDocuments:top6";
    private static final String CACHE_COMPOSITE_CAFFEINE_NAME = "hotDocuments";
    private static final String CACHE_COMPOSITE_CAFFEINE_KEY = "top6";
    private static final String CACHE_LATEST_REDIS_KEY = "stats:latestDocuments:top6";
    private static final String CACHE_LATEST_CAFFEINE_NAME = "latestDocuments";
    private static final String CACHE_LATEST_CAFFEINE_KEY = "top6";

    private static final String CACHE_OVERVIEW = "stats:overview";
    private static final String CACHE_ADMIN_OVERVIEW = "stats:adminOverview";
    private static final String CACHE_DASHBOARD = "stats:dashboard";
    private static final String CACHE_CATEGORY_DIST = "stats:categoryDistribution";
    private static final String CACHE_HOT_DOCS = "stats:hotDocuments";
    private static final String CACHE_ACTIVE_USERS = "stats:activeUsers";
    private static final String CACHE_DOC_TREND = "stats:documentTrend";
    private static final String CACHE_USER_ACTIVITY = "stats:userActivity";

    // ======================== Business constants ========================

    /** Default number of popular documents to return */
    private static final int DEFAULT_HOT_LIMIT = 10;
    /** Default number of active users to return */
    private static final int DEFAULT_ACTIVE_USER_LIMIT = 10;
    /** Maximum number of results to return */
    private static final int MAX_LIMIT = 100;
    /** Default activity scores: create document +10, comment +5, view +1 */
    private static final int SCORE_DOCUMENT = 10;
    private static final int SCORE_COMMENT = 5;
    private static final int SCORE_VIEW = 1;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Popular document composite score weights: view 1, like 3, favorite 5 */
    private static final int HOT_SCORE_LIKE = 3;
    private static final int HOT_SCORE_FAVORITE = 5;

    // ======================== Data overview ========================

    @Override
    public OverviewVO getOverview() {
        return overviewCache.get("overview");
    }

    /**
     * Loads the data overview from the database (only called when the cache has expired)
     */
    private OverviewVO loadOverview() {
        log.debug("Querying data overview (in-memory cache miss, refreshed every 10 minutes)");

        OverviewVO overview = new OverviewVO();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        overview.setTotalDocuments(nullSafe(documentMapper.countAll()));
        overview.setTotalUsers(nullSafe(userMapper.countAll()));
        overview.setTodayDocuments(nullSafe(documentMapper.countByDateRange(todayStart, todayEnd)));
        overview.setTodayUsers(nullSafe(userMapper.countByDateRange(todayStart, todayEnd)));
        overview.setTotalViews(nullSafe(viewMapper.countAll()));
        overview.setTodayViews(nullSafe(viewMapper.countByDateRange(todayStart, todayEnd)));
        overview.setTotalLikes(nullSafe(documentMapper.sumLikeCount()));
        overview.setTotalFavorites(nullSafe(documentMapper.sumFavoriteCount()));
        overview.setTotalComments(nullSafe(commentMapper.selectCount(null)));
        overview.setPendingReviews(nullSafe(documentMapper.countByStatus(0)));
        overview.setAiSearchCount(nullSafe(aiStatisticsMapper.countConversations()));
        overview.setAiQaCount(nullSafe(aiStatisticsMapper.countUserMessages()));
        overview.setActiveUserCount(nullSafe(viewMapper.countActiveUsers(LocalDateTime.now().minusDays(30))));

        return overview;
    }

    // ======================== Document trend ========================

    @Override
    @Cacheable(value = CACHE_DOC_TREND, key = "#startDate + '_' + #endDate + '_' + #type",
            unless = "#result == null || #result.isEmpty()")
    public List<TrendVO> getDocumentTrend(LocalDate startDate, LocalDate endDate, String type) {
        log.debug("Querying document trend: startDate={}, endDate={}, type={}", startDate, endDate, type);

        LocalDateTime start = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(endDate, LocalTime.MAX);

        // Uses the XML Mapper's GROUP BY batch query to fetch all dates in a single SQL statement
        List<DailyCount> dailyCounts = queryDailyCounts(type, start, end);
        Map<String, Long> dateCountMap = toDateCountMap(dailyCounts);

        // Fill in every day within the date range (padding with 0 for dates with no data)
        List<TrendVO> trends = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String dateStr = current.toString();
            trends.add(TrendVO.builder()
                    .date(dateStr)
                    .count(dateCountMap.getOrDefault(dateStr, 0L))
                    .build());
            current = current.plusDays(1);
        }

        return trends;
    }

    /**
     * Queries daily statistics data by type
     */
    private List<DailyCount> queryDailyCounts(String type, LocalDateTime start, LocalDateTime end) {
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        switch (type) {
            case "create":
                return documentMapper.countDailyDocuments(start, end);
            case "view":
                return documentStatisticsAggMapper.countDailyViews(startDate, endDate);
            case "like":
            case "favorite":
                log.debug("Trend type {} does not yet support daily aggregation", type);
                return Collections.emptyList();
            default:
                log.warn("Unknown trend type: {}", type);
                return Collections.emptyList();
        }
    }

    /**
     * Converts a list of DailyCount into a { date → count } map
     */
    private Map<String, Long> toDateCountMap(List<DailyCount> dailyCounts) {
        if (dailyCounts == null || dailyCounts.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> map = new LinkedHashMap<>();
        for (DailyCount dc : dailyCounts) {
            if (dc.getDate() != null && dc.getCount() != null) {
                map.put(dc.getDate(), dc.getCount());
            }
        }
        return map;
    }

    // ======================== User activity ========================

    @Override
    @Cacheable(value = CACHE_USER_ACTIVITY,
            key = "#startDate + '_' + #endDate",
            unless = "#result == null || #result.isEmpty()")
    public List<UserActivityVO> getUserActivity(LocalDate startDate, LocalDate endDate) {
        log.debug("Querying user activity: startDate={}, endDate={}", startDate, endDate);

        LocalDateTime start = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(endDate, LocalTime.MAX);

        // Get all users
        List<UserStatistics> users = userMapper.selectList(null);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch-prefetch each user's data counts (avoiding N+1 queries)
        Map<Long, Long> docCountMap = buildDocCountMap(start, end);
        Map<Long, Long> commentCountMap = buildCommentCountMap(start, end);
        Map<Long, Long> viewCountMap = buildViewCountMap(start, end);

        return users.stream()
                .map(user -> {
                    Long userId = user.getId();
                    long docCount = docCountMap.getOrDefault(userId, 0L);
                    long comCount = commentCountMap.getOrDefault(userId, 0L);
                    long vCount = viewCountMap.getOrDefault(userId, 0L);

                    return UserActivityVO.builder()
                            .userId(userId)
                            .username(user.getUsername())
                            .documentCount(docCount)
                            .commentCount(comCount)
                            .viewCount(vCount)
                            .activityScore(calcActivityScore(docCount, comCount, vCount))
                            .build();
                })
                .sorted(Comparator.comparing(UserActivityVO::getActivityScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Builds a user → document-count mapping
     */
    private Map<Long, Long> buildDocCountMap(LocalDateTime start, LocalDateTime end) {
        List<DocumentStatistics> docs = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentStatistics>()
                        .between(DocumentStatistics::getCreatedAt, start, end)
                        .select(DocumentStatistics::getAuthorId));
        return docs.stream()
                .filter(d -> d.getAuthorId() != null)
                .collect(Collectors.groupingBy(DocumentStatistics::getAuthorId, Collectors.counting()));
    }

    /**
     * Builds a user → comment-count mapping
     */
    private Map<Long, Long> buildCommentCountMap(LocalDateTime start, LocalDateTime end) {
        List<CommentStatistics> comments = commentMapper.selectList(
                new LambdaQueryWrapper<CommentStatistics>()
                        .between(CommentStatistics::getCreatedAt, start, end)
                        .select(CommentStatistics::getUserId));
        return comments.stream()
                .filter(c -> c.getUserId() != null)
                .collect(Collectors.groupingBy(CommentStatistics::getUserId, Collectors.counting()));
    }

    /**
     * Builds a user → view-count mapping
     */
    private Map<Long, Long> buildViewCountMap(LocalDateTime start, LocalDateTime end) {
        List<ViewStatistics> views = viewMapper.selectList(
                new LambdaQueryWrapper<ViewStatistics>()
                        .between(ViewStatistics::getCreatedAt, start, end)
                        .select(ViewStatistics::getUserId));
        return views.stream()
                .filter(v -> v.getUserId() != null)
                .collect(Collectors.groupingBy(ViewStatistics::getUserId, Collectors.counting()));
    }

    /**
     * Computes a user's activity score
     */
    private double calcActivityScore(long docCount, long commentCount, long viewCount) {
        return (docCount * SCORE_DOCUMENT + commentCount * SCORE_COMMENT + viewCount * SCORE_VIEW) / 10.0;
    }

    // ======================== Category distribution (P0: mock → real query) ========================

    @Override
    @Cacheable(value = CACHE_CATEGORY_DIST, key = "'distribution'",
            unless = "#result == null || #result.isEmpty()")
    public List<CategoryDistributionVO> getCategoryDistribution() {
        log.debug("Querying category distribution (cache miss)");

        // Uses the XML Mapper's predefined countByCategory query in a single call
        List<IdCount> categoryCounts = documentMapper.countByCategory();
        if (categoryCounts == null || categoryCounts.isEmpty()) {
            return Collections.emptyList();
        }

        // Compute the total document count for percentage calculation
        long total = categoryCounts.stream()
                .mapToLong(ic -> nullSafe(ic.getCount()))
                .sum();
        if (total <= 0) {
            return Collections.emptyList();
        }

        // Get the category name mapping
        Map<Long, String> categoryNameMap = buildCategoryNameMap();

        return categoryCounts.stream()
                .map(ic -> {
                    Long categoryId = ic.getId();
                    long count = nullSafe(ic.getCount());
                    String categoryName = categoryId != null
                            ? categoryNameMap.getOrDefault(categoryId, "Unnamed")
                            : "Uncategorized";

                    return CategoryDistributionVO.builder()
                            .categoryId(categoryId)
                            .categoryName(categoryName)
                            .documentCount(count)
                            .percentage(calcPercentage(count, total))
                            .build();
                })
                .sorted(Comparator.comparing(CategoryDistributionVO::getDocumentCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Queries the real category name mapping from the kb_category view
     */
    private Map<Long, String> buildCategoryNameMap() {
        Map<Long, String> nameMap = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, category_name FROM kb_category WHERE deleted = 0");
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Long id = toLong(row.get("id"));
                    String name = (String) row.get("category_name");
                    if (id != null) {
                        nameMap.put(id, name != null ? name : "Unnamed");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to build category name mapping: {}", e.getMessage());
        }
        return nameMap;
    }

    // ======================== Popular documents (P0: mock → real query) ========================

    @Override
    @Cacheable(value = CACHE_HOT_DOCS, key = "#type + '_' + #size",
            unless = "#result == null || #result.isEmpty()",
            condition = "!'composite'.equals(#type)")
    public List<HotDocumentVO> getHotDocuments(String type, Integer size) {
        log.debug("Querying popular documents: type={}, size={}", type, size);

        int limit = Math.min(size != null ? size : DEFAULT_HOT_LIMIT, MAX_LIMIT);

        // Composite-popularity type: uses the three-tier read path L1 Caffeine → L2 Redis → DB
        if ("composite".equals(type)) {
            return getCompositeHotDocuments(limit);
        }

        List<DocumentStatistics> docs = queryHotDocsByType(type, limit);

        if (docs == null || docs.isEmpty()) {
            return Collections.emptyList();
        }

        return docs.stream()
                .map(doc -> {
                    long statsValue = extractStatsValue(doc, type);
                    return HotDocumentVO.builder()
                            .documentId(doc.getId())
                            .title(doc.getTitle())
                            .authorId(doc.getAuthorId())
                            .authorName("") // Can be joined from the user table later
                            .categoryId(doc.getCategoryId())
                            .categoryName("") // Can be joined from the category table later
                            .viewCount(nullSafe(doc.getViewCount()))
                            .likeCount(nullSafe(doc.getLikeCount()))
                            .favoriteCount(nullSafe(doc.getFavoriteCount()))
                            .commentCount(0L) // Comment count is not joined for now
                            .statisticsValue(statsValue)
                            .build();
                })
                .sorted(Comparator.comparing(HotDocumentVO::getStatisticsValue).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Queries popular documents by type
     */
    private List<DocumentStatistics> queryHotDocsByType(String type, int limit) {
        if ("like".equalsIgnoreCase(type)) {
            return documentMapper.selectMostLikedDocuments(limit);
        }
        if ("favorite".equalsIgnoreCase(type)) {
            return documentMapper.selectMostFavoritedDocuments(limit);
        }
        // Defaults to sorting by view count
        return documentMapper.selectMostViewedDocuments(limit);
    }

    /**
     * Extracts the statistic value corresponding to the sort type from a document record
     */
    private long extractStatsValue(DocumentStatistics doc, String type) {
        if ("like".equalsIgnoreCase(type)) {
            return nullSafe(doc.getLikeCount());
        }
        if ("favorite".equalsIgnoreCase(type)) {
            return nullSafe(doc.getFavoriteCount());
        }
        return nullSafe(doc.getViewCount());
    }

    // ======================== Composite-popularity documents (scheduled precomputation + multi-tier cache) ========================

    /**
     * Gets the composite-popularity ranked documents
     *
     * <p>Read path: Caffeine (L1) → Redis (L2) → real-time DB computation (fallback)</p>
     * <p>Data is precomputed every 30 minutes by {@code HotDocumentsCacheTask} and written to the cache</p>
     */
    private List<HotDocumentVO> getCompositeHotDocuments(int limit) {
        // 1. Check the Caffeine local cache (L1)
        Cache caffeineCache = caffeineCacheManager.getCache(CACHE_COMPOSITE_CAFFEINE_NAME);
        if (caffeineCache != null) {
            Cache.ValueWrapper wrapper = caffeineCache.get(CACHE_COMPOSITE_CAFFEINE_KEY);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                List<HotDocumentVO> cached = (List<HotDocumentVO>) wrapper.get();
                if (cached != null && !cached.isEmpty()) {
                    log.debug("Composite-popularity documents hit the Caffeine local cache, {} entries", cached.size());
                    return cached.size() > limit ? cached.subList(0, limit) : cached;
                }
            }
        }

        // 2. Check the Redis cache (L2)
        try {
            @SuppressWarnings("unchecked")
            List<HotDocumentVO> redisCached = (List<HotDocumentVO>) redisTemplate.opsForValue()
                    .get(CACHE_COMPOSITE_REDIS_KEY);
            if (redisCached != null && !redisCached.isEmpty()) {
                log.debug("Composite-popularity documents hit the Redis cache, {} entries", redisCached.size());
                return redisCached.size() > limit ? redisCached.subList(0, limit) : redisCached;
            }
        } catch (Exception e) {
            log.warn("Failed to read the Redis composite-popularity cache: {}", e.getMessage());
        }

        // 3. Fallback: compute from the database in real time
        log.info("Composite-popularity documents cache miss, performing a real-time database query");
        return computeCompositeOnDemand(limit);
    }

    /**
     * Computes the top N documents by composite popularity in real time from the database (cache fallback logic)
     *
     * <p>Popularity formula: viewCount * 1 + likeCount * 3 + favoriteCount * 5</p>
     */
    private List<HotDocumentVO> computeCompositeOnDemand(int limit) {
        List<DocumentStatistics> allDocs = documentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocumentStatistics>()
                        .eq(DocumentStatistics::getStatus, 1)
                        .eq(DocumentStatistics::getDeleted, 0)
                        .gt(DocumentStatistics::getViewCount, 0)
                        .orderByDesc(DocumentStatistics::getViewCount)
                        .last("LIMIT 500"));

        if (allDocs == null || allDocs.isEmpty()) {
            return Collections.emptyList();
        }

        List<DocumentStatistics> topDocs = allDocs.stream()
                .sorted((a, b) -> Long.compare(
                        compositeScore(b), compositeScore(a)))
                .limit(limit)
                .collect(Collectors.toList());

        // Batch-query summaries
        Map<Long, String> summaryMap = buildSummaryMapForDocs(topDocs);

        return topDocs.stream()
                .map(doc -> HotDocumentVO.builder()
                        .documentId(doc.getId())
                        .title(doc.getTitle())
                        .authorId(doc.getAuthorId())
                        .authorName("")
                        .categoryId(doc.getCategoryId())
                        .categoryName("")
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
     * Batch-builds a document ID → summary mapping
     */
    private Map<Long, String> buildSummaryMapForDocs(List<DocumentStatistics> docs) {
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

    private long compositeScore(DocumentStatistics doc) {
        return nullSafe(doc.getViewCount()) * SCORE_VIEW
                + nullSafe(doc.getLikeCount()) * HOT_SCORE_LIKE
                + nullSafe(doc.getFavoriteCount()) * HOT_SCORE_FAVORITE;
    }

    // ======================== Latest documents (scheduled precomputation + multi-tier cache) ========================

    @Override
    public List<HotDocumentVO> getLatestDocuments(Integer size) {
        int limit = Math.min(size != null ? size : 6, MAX_LIMIT);

        // 1. Check the Caffeine local cache (L1)
        Cache caffeineCache = caffeineCacheManager.getCache(CACHE_LATEST_CAFFEINE_NAME);
        if (caffeineCache != null) {
            Cache.ValueWrapper wrapper = caffeineCache.get(CACHE_LATEST_CAFFEINE_KEY);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                List<HotDocumentVO> cached = (List<HotDocumentVO>) wrapper.get();
                if (cached != null && !cached.isEmpty()) {
                    log.debug("Latest documents hit the Caffeine local cache, {} entries", cached.size());
                    return cached.size() > limit ? cached.subList(0, limit) : cached;
                }
            }
        }

        // 2. Check the Redis cache (L2)
        try {
            @SuppressWarnings("unchecked")
            List<HotDocumentVO> redisCached = (List<HotDocumentVO>) redisTemplate.opsForValue()
                    .get(CACHE_LATEST_REDIS_KEY);
            if (redisCached != null && !redisCached.isEmpty()) {
                log.debug("Latest documents hit the Redis cache, {} entries", redisCached.size());
                return redisCached.size() > limit ? redisCached.subList(0, limit) : redisCached;
            }
        } catch (Exception e) {
            log.warn("Failed to read the Redis latest-documents cache: {}", e.getMessage());
        }

        // 3. Fallback: query the database in real time
        log.info("Latest-documents cache miss, performing a real-time database query");
        return computeLatestOnDemand(limit);
    }

    private List<HotDocumentVO> computeLatestOnDemand(int limit) {
        List<DocumentStatistics> latestDocs = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentStatistics>()
                        .eq(DocumentStatistics::getStatus, 1)
                        .eq(DocumentStatistics::getDeleted, 0)
                        .orderByDesc(DocumentStatistics::getCreatedAt)
                        .last("LIMIT " + limit));

        if (latestDocs == null || latestDocs.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> summaryMap = buildSummaryMapForDocs(latestDocs);

        return latestDocs.stream()
                .map(doc -> HotDocumentVO.builder()
                        .documentId(doc.getId())
                        .title(doc.getTitle())
                        .authorId(doc.getAuthorId())
                        .authorName("")
                        .categoryId(doc.getCategoryId())
                        .categoryName("")
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

    // ======================== Active users (P0: mock → real query) ========================

    @Override
    @Cacheable(value = CACHE_ACTIVE_USERS, key = "#type + '_' + #size",
            unless = "#result == null || #result.isEmpty()")
    public List<ActiveUserVO> getActiveUsers(String type, Integer size) {
        log.debug("Querying active users: type={}, size={}", type, size);

        int limit = Math.min(size != null ? size : DEFAULT_ACTIVE_USER_LIMIT, MAX_LIMIT);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Use a different aggregate data source depending on the type
        List<IdCount> topUsers = queryTopUsersByType(type, thirtyDaysAgo, limit);
        if (topUsers == null || topUsers.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch-fetch user information
        List<Long> userIds = topUsers.stream()
                .map(IdCount::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, UserStatistics> userMap = buildUserMap(userIds);

        return topUsers.stream()
                .map(ic -> {
                    Long userId = ic.getId();
                    long statsValue = nullSafe(ic.getCount());
                    UserStatistics user = userMap.get(userId);

                    return ActiveUserVO.builder()
                            .userId(userId)
                            .username(user != null ? user.getUsername() : "Unknown user")
                            .realName(user != null ? user.getRealName() : "")
                            .avatar(user != null ? user.getAvatar() : "")
                            .documentCount(0L)
                            .commentCount(0L)
                            .viewCount(0L)
                            .statisticsValue(statsValue)
                            .build();
                })
                .sorted(Comparator.comparing(ActiveUserVO::getStatisticsValue).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Queries the popular user list by type
     */
    private List<IdCount> queryTopUsersByType(String type, LocalDateTime since, int limit) {
        if ("create".equalsIgnoreCase(type)) {
            // Finds the most active authors by document creation count, using the kb_document view
            return documentMapper.selectTopAuthors(limit);
        }
        if ("comment".equalsIgnoreCase(type)) {
            return commentMapper.selectTopCommenters(limit);
        }
        if ("view".equalsIgnoreCase(type)) {
            try {
                List<IdCount> result = userStatisticsAggMapper.selectTopActiveUsers(
                        since.toLocalDate(), LocalDate.now(), limit);
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("Failed to query the pre-aggregated user statistics table, falling back to kb_view_history: {}", e.getMessage());
            }
            return viewMapper.selectMostActiveViewers(limit);
        }
        // Defaults to sorting by document creation count
        return documentMapper.selectTopAuthors(limit);
    }

    /**
     * Batch-builds a user ID → user info mapping
     */
    private Map<Long, UserStatistics> buildUserMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserStatistics> users = userMapper.selectBatchIds(userIds);
        if (users == null) {
            return Collections.emptyMap();
        }
        return users.stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(UserStatistics::getId, u -> u, (a, b) -> a));
    }

    // ======================== Admin overview ========================

    @Override
    @Cacheable(value = CACHE_ADMIN_OVERVIEW, key = "'adminOverview'", unless = "#result == null")
    public AdminOverviewVO getAdminOverview() {
        log.debug("Querying admin overview (cache miss)");

        AdminOverviewVO vo = new AdminOverviewVO();
        vo.setTotalDocuments(nullSafe(documentMapper.countAll()));
        vo.setTotalUsers(nullSafe(userMapper.countAll()));
        vo.setPendingReviews(nullSafe(documentMapper.countByStatus(0)));
        vo.setTotalComments(nullSafe(commentMapper.selectCount(null)));
        vo.setTotalViews(nullSafe(viewMapper.countAll()));
        vo.setTotalLikes(nullSafe(documentMapper.sumLikeCount()));
        vo.setTotalFavorites(nullSafe(documentMapper.sumFavoriteCount()));
        vo.setAiSearchCount(nullSafe(aiStatisticsMapper.countConversations()));
        vo.setAiQaCount(nullSafe(aiStatisticsMapper.countUserMessages()));
        vo.setSystemHealth(98.0); // TODO: compute dynamically via a health-check endpoint

        return vo;
    }

    // ======================== Dashboard data ========================

    @Override
    @Cacheable(value = CACHE_DASHBOARD, key = "'dashboard'", unless = "#result == null")
    public DashboardVO getDashboardData() {
        log.debug("Querying dashboard data (cache miss)");

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        return DashboardVO.builder()
                .overview(getOverview())
                .documentTrend(getDocumentTrend(startDate, endDate, "create"))
                .categoryDistribution(getCategoryDistribution())
                .hotDocuments(getHotDocuments("view", DEFAULT_HOT_LIMIT))
                .activeUsers(getActiveUsers("create", DEFAULT_ACTIVE_USER_LIMIT))
                .build();
    }

    // ======================== Utility methods ========================

    /**
     * Safely converts to Long, avoiding null and type-cast exceptions
     */
    private Long toLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Safe null → 0L conversion
     */
    private long nullSafe(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * Computes a percentage, rounded to one decimal place
     */
    private double calcPercentage(long count, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((double) count / total * 1000.0) / 10.0;
    }
}
