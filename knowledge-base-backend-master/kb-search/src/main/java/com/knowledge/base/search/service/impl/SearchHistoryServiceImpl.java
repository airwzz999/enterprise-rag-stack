package com.knowledge.base.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.search.entity.SearchHistory;
import com.knowledge.base.search.mapper.SearchHistoryMapper;
import com.knowledge.base.search.service.SearchHistoryService;
import com.knowledge.base.search.vo.SearchHistoryVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Search history Service implementation
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class SearchHistoryServiceImpl implements SearchHistoryService {

    @Resource
    private SearchHistoryMapper searchHistoryMapper;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    /** {@inheritDoc} */
    @Override
    public List<SearchHistoryVO> getSearchHistory(Long userId) {
        LambdaQueryWrapper<SearchHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SearchHistory::getUserId, userId);
        queryWrapper.orderByDesc(SearchHistory::getCreatedAt);

        List<SearchHistory> histories = searchHistoryMapper.selectList(queryWrapper);

        // Group by keyword, keeping the most recent record
        Map<String, SearchHistory> keywordMap = histories.stream()
            .collect(Collectors.toMap(
                SearchHistory::getKeyword,
                h -> h,
                (h1, h2) -> h1.getCreatedAt().isAfter(h2.getCreatedAt()) ? h1 : h2
            ));

        return keywordMap.values().stream()
            .sorted(Comparator.comparing(SearchHistory::getCreatedAt).reversed())
            .limit(20)
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean clearSearchHistory(Long userId) {
        LambdaQueryWrapper<SearchHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SearchHistory::getUserId, userId);
        return searchHistoryMapper.delete(queryWrapper) >= 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSearchHistory(Long historyId, Long userId) {
        LambdaQueryWrapper<SearchHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SearchHistory::getId, historyId);
        queryWrapper.eq(SearchHistory::getUserId, userId);
        return searchHistoryMapper.delete(queryWrapper) > 0;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getHotSearch() {
        // Get popular searches from the last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        LambdaQueryWrapper<SearchHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(SearchHistory::getCreatedAt, sevenDaysAgo);
        queryWrapper.orderByDesc(SearchHistory::getSearchCount);
        queryWrapper.last("LIMIT 10");

        return searchHistoryMapper.selectList(queryWrapper).stream()
            .map(SearchHistory::getKeyword)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSearchHistory(Long userId, String keyword) {
        // Check whether a record with the same keyword already exists
        LambdaQueryWrapper<SearchHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SearchHistory::getUserId, userId);
        queryWrapper.eq(SearchHistory::getKeyword, keyword);

        SearchHistory existingHistory = searchHistoryMapper.selectOne(queryWrapper);

        if (existingHistory != null) {
            // Update the search count and timestamp
            existingHistory.setSearchCount(existingHistory.getSearchCount() + 1);
            existingHistory.setCreatedAt(LocalDateTime.now());
            searchHistoryMapper.updateById(existingHistory);
        } else {
            // Create a new search history record
            SearchHistory newHistory = new SearchHistory();
            newHistory.preInsert(); // Generate the ID with the Snowflake algorithm
            newHistory.setUserId(userId);
            newHistory.setKeyword(keyword);
            newHistory.setSearchCount(1);
            newHistory.setCreatedAt(LocalDateTime.now());
            searchHistoryMapper.insert(newHistory);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void saveSearchHistoryAsync(Long userId, String keyword) {
        CompletableFuture.runAsync(() -> {
            try {
                saveSearchHistory(userId, keyword);
            } catch (Exception e) {
                log.warn("Failed to save search history: keyword={}, error={}", keyword, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Convert to a VO
     *
     * @param history the search history entity
     * @return the search history VO
     */
    private SearchHistoryVO convertToVO(SearchHistory history) {
        SearchHistoryVO vo = new SearchHistoryVO();
        BeanUtils.copyProperties(history, vo);
        return vo;
    }
}
