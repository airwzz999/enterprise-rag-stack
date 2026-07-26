package com.knowledge.base.search.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import com.knowledge.base.search.service.SearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Search history Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/history")
@Tag(name = "Search History", description = "Search history management endpoints")
public class SearchHistoryController {

    @Resource
    private SearchHistoryService searchHistoryService;

    /**
     * Delete a specific search history entry
     *
     * @param historyId the history ID
     * @param request   the HTTP request
     * @return whether it succeeded
     */
    @DeleteMapping("/{historyId}")
    @Operation(summary = "Delete search history", description = "Delete the specified search history record")
    public Result<Boolean> deleteSearchHistory(
            @PathVariable Long historyId,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        Boolean success = searchHistoryService.deleteSearchHistory(historyId, userId);
        return Result.success(success);
    }

    /**
     * Get popular searches
     *
     * @return the list of popular searches
     */
    @GetMapping("/hot")
    @Operation(summary = "Get popular searches", description = "Get the system's popular search keywords")
    public Result<List<String>> getHotSearch() {
        List<String> hotSearch = searchHistoryService.getHotSearch();
        return Result.success(hotSearch);
    }

}
