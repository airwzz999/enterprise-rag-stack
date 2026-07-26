package com.knowledge.base.search.service;

import com.knowledge.base.search.vo.SearchHistoryVO;

import java.util.List;

/**
 * Search history Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface SearchHistoryService {

    /**
     * Get search history
     *
     * @param userId the user ID
     * @return the search history list
     */
    List<SearchHistoryVO> getSearchHistory(Long userId);

    /**
     * Clear search history
     *
     * @param userId the user ID
     * @return whether it succeeded
     */
    Boolean clearSearchHistory(Long userId);

    /**
     * Delete a search history entry
     *
     * @param historyId the history ID
     * @param userId    the user ID
     * @return whether it succeeded
     */
    Boolean deleteSearchHistory(Long historyId, Long userId);

    /**
     * Get popular searches
     *
     * @return the list of popular search keywords
     */
    List<String> getHotSearch();

    /**
     * Save a search history entry
     *
     * @param userId  the user ID
     * @param keyword the search keyword
     */
    void saveSearchHistory(Long userId, String keyword);

    /**
     * Save a search history entry asynchronously (does not block the main flow)
     *
     * @param userId  the user ID
     * @param keyword the search keyword
     */
    void saveSearchHistoryAsync(Long userId, String keyword);
}
