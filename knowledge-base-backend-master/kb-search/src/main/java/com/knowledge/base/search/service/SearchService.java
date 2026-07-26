package com.knowledge.base.search.service;

import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.search.dto.SearchRequestDTO;
import com.knowledge.base.search.vo.SearchResultVO;
import com.knowledge.base.search.vo.SearchSuggestVO;

import java.util.List;
import java.util.Map;

/**
 * Search Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface SearchService {

    /**
     * Full-text search
     *
     * @param dto the search request
     * @return the search results
     */
    PageResult<SearchResultVO> search(SearchRequestDTO dto);

    /**
     * Advanced search
     *
     * @param dto the search request
     * @return the search results
     */
    PageResult<SearchResultVO> advancedSearch(SearchRequestDTO dto);

    /**
     * Search suggestions
     *
     * @param keyword the keyword
     * @param size    the number of results to return
     * @return the list of suggestions
     */
    List<SearchSuggestVO> suggest(String keyword, Integer size);

    /**
     * Save a document index
     *
     * @param documentId the document ID
     */
    void indexDocument(Long documentId);

    /**
     * Index directly into ES from document data (internal call from the kb-document service)
     *
     * @param docData the document data Map
     */
    void indexDocumentData(Map<String, Object> docData);

    /**
     * Batch-save document indexes
     *
     * @param documentIds the list of document IDs
     */
    void batchIndexDocuments(List<Long> documentIds);

    /**
     * Delete a document index
     *
     * @param documentId the document ID
     */
    void deleteDocument(Long documentId);

    /**
     * Batch-delete document indexes
     *
     * @param documentIds the list of document IDs
     */
    void batchDeleteDocuments(List<Long> documentIds);

    /**
     * Rebuild the index
     */
    void rebuildIndex();
}
