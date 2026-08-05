package com.knowledge.base.search.controller;

import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import com.knowledge.base.search.dto.SearchRequestDTO;
import com.knowledge.base.search.service.SearchHistoryService;
import com.knowledge.base.search.service.SearchService;
import com.knowledge.base.search.vo.SearchHistoryVO;
import com.knowledge.base.search.vo.SearchResultVO;
import com.knowledge.base.search.vo.SearchSuggestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Search Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Tag(name = "Search Service", description = "Search-related endpoints")
public class SearchController {

    private final SearchService searchService;
    private final SearchHistoryService searchHistoryService;

    /**
     * Full-text search
     */
    @PostMapping
    @Operation(summary = "Full-text search", description = "Perform a full-text search")
    public Result<PageResult<SearchResultVO>> search(@Valid @RequestBody SearchRequestDTO dto,
                                                      HttpServletRequest request) {
        PageResult<SearchResultVO> result = searchService.search(dto);
        searchHistoryService.saveSearchHistoryAsync(
                UserContextUtil.getUserIdFromHeader(request), dto.getKeyword());
        return Result.success(result);
    }

    /**
     * Advanced search
     */
    @PostMapping("/advanced")
    @Operation(summary = "Advanced search", description = "Perform an advanced search")
    public Result<PageResult<SearchResultVO>> advancedSearch(@Valid @RequestBody SearchRequestDTO dto,
                                                              HttpServletRequest request) {
        PageResult<SearchResultVO> result = searchService.advancedSearch(dto);
        searchHistoryService.saveSearchHistoryAsync(
                UserContextUtil.getUserIdFromHeader(request), dto.getKeyword());
        return Result.success(result);
    }

    /**
     * Search suggestions
     */
    @GetMapping("/suggest")
    @Operation(summary = "Search suggestions", description = "Get search suggestions")
    public Result<List<SearchSuggestVO>> suggest(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") Integer size) {
        List<SearchSuggestVO> suggestions = searchService.suggest(keyword, size);
        return Result.success(suggestions);
    }

    /**
     * Popular searches
     */
    @GetMapping("/hot")
    @Operation(summary = "Popular searches", description = "Get the system's popular search keywords")
    public Result<List<String>> hotSearch() {
        List<String> hotSearches = searchHistoryService.getHotSearch();
        return Result.success(hotSearches);
    }

    /**
     * Get the current user's search history
     */
    @GetMapping("/history")
    @Operation(summary = "Search history", description = "Get the current user's search history")
    public Result<List<SearchHistoryVO>> searchHistory(HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        List<SearchHistoryVO> history = searchHistoryService.getSearchHistory(userId);
        return Result.success(history);
    }

    /**
     * Clear the current user's search history
     */
    @DeleteMapping("/history")
    @Operation(summary = "Clear search history", description = "Clear the current user's search history")
    public Result<Boolean> clearSearchHistory(HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        Boolean success = searchHistoryService.clearSearchHistory(userId);
        return Result.success(success);
    }

    /**
     * Rebuild the index
     *
     * <p>Drops and recreates the whole ES index - restricted to admins, since any
     * authenticated user being able to trigger this is a denial-of-service risk against
     * search for every user.</p>
     */
    @PostMapping("/index/rebuild")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Rebuild index", description = "Rebuild the search index")
    public Result<String> rebuildIndex() {
        searchService.rebuildIndex();
        return Result.success("Index rebuild task submitted");
    }

    /**
     * Index a document (for internal use by kb-document)
     *
     * <p>Writes the caller-supplied map directly into the shared search index with no
     * validation of its fields, so it must not be reachable by ordinary authenticated
     * users - otherwise any user could inject fake documents or overwrite another
     * document's indexed content/links for everyone's search results.</p>
     *
     * @param docData the document data
     * @return whether it succeeded
     */
    @PostMapping("/index/document")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Index document", description = "Index document data directly into ES (internal call from kb-document)")
    public Result<Boolean> indexDocument(@RequestBody Map<String, Object> docData) {
        searchService.indexDocumentData(docData);
        return Result.success(true);
    }

    /**
     * Delete a document index (for internal use by kb-document)
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    @DeleteMapping("/index/document/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete document index", description = "Delete the document index from ES (internal call from kb-document)")
    public Result<Boolean> deleteDocumentIndex(@PathVariable Long documentId) {
        searchService.deleteDocument(documentId);
        return Result.success(true);
    }

}
