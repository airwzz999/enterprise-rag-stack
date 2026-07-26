package com.knowledge.base.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.search.dto.SearchRequestDTO;
import com.knowledge.base.search.entity.DocumentIndex;
import com.knowledge.base.search.feign.RagSearchFeignClient;
import com.knowledge.base.search.feign.RagSearchItemVO;
import com.knowledge.base.search.feign.RagSearchRequest;
import com.knowledge.base.search.service.SearchService;
import com.knowledge.base.search.vo.SearchResultVO;
import com.knowledge.base.search.vo.SearchSuggestVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Search service implementation
 *
 * <p>Provides two search modes:</p>
 * <ol>
 *   <li><b>Keyword search</b>: BM25 full-text retrieval based on ES multi_match, with highlighting</li>
 *   <li><b>Hybrid intelligent search</b>: BM25 + kNN + RRF fusion retrieval via a Feign call to kb-ai</li>
 * </ol>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    /** ES index name */
    private static final String INDEX_NAME = "kb_document";

    // ==================== Dependency injection ====================

    /** Spring Data ES operations template (query, index, delete) */
    private final ElasticsearchOperations elasticsearchOperations;

    /** ES low-level client (native queries, index management) */
    private final ElasticsearchClient esClient;

    /** kb-ai hybrid search Feign client */
    private final RagSearchFeignClient ragSearchFeignClient;

    /** JSON serialization/deserialization */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Default user ID for internal system calls */
    private static final String SYSTEM_USER_ID = "1";

    /** kb-document service address (used for index synchronization) */
    @Value("${kb-document.base-url:http://localhost:8082}")
    private String kbDocumentBaseUrl;

    // ==================== Search entry point ====================

    /** {@inheritDoc} */
    @Override
    public PageResult<SearchResultVO> search(SearchRequestDTO dto) {
        // Hybrid intelligent search
        if ("hybrid".equals(dto.getSearchMode())) {
            return searchHybrid(dto);
        }
        // Keyword search
        return searchKeyword(dto);
    }

    /** {@inheritDoc} */
    @Override
    public PageResult<SearchResultVO> advancedSearch(SearchRequestDTO dto) {
        log.info("Advanced search: keyword={}", dto.getKeyword());
        return search(dto);
    }

    // ==================== Keyword search ====================

    /**
     * Keyword search (BM25 full-text retrieval + ES highlighting)
     *
     * <p>Builds a multi_match query via the native ES client, applying weighted matching
     * across the title, summary, and content fields, with title given the highest weight.
     * Also configures ES highlight to return highlighted snippets.</p>
     *
     * @param dto the search request (contains the keyword and pagination parameters)
     * @return the paginated search results
     */
    private PageResult<SearchResultVO> searchKeyword(SearchRequestDTO dto) {
        log.info("Keyword search: keyword={}", dto.getKeyword());

        try {
            String keyword = dto.getKeyword();

            // Build the ES query: weighted multi_match + docStatus filter + highlight
            SearchResponse<Map> response = esClient.search(s -> {
                s.index(INDEX_NAME)
                 .from((dto.getCurrent() - 1) * dto.getSize())
                 .size(dto.getSize());

                // With a keyword: weighted multi-field matching + published-only filter
                if (StringUtils.hasText(keyword)) {
                    s.query(q -> q.bool(b -> b
                            .must(m -> m.multiMatch(mm -> mm
                                    .query(keyword)
                                    .fields("title^2", "summary^1.5", "content")))
                            .filter(f -> f.term(t -> t.field("docStatus").value(1)))));
                } else {
                    // Without a keyword: only filter to published documents
                    s.query(q -> q.bool(b -> b
                            .filter(f -> f.term(t -> t.field("docStatus").value(1)))));
                }

                // Highlight configuration: title and summary return the full field, content returns 3 fragments
                s.highlight(h -> h
                        .preTags("<em>").postTags("</em>")
                        .fields("title", hf -> hf.numberOfFragments(0))
                        .fields("summary", hf -> hf.numberOfFragments(0))
                        .fields("content", hf -> hf.fragmentSize(150).numberOfFragments(3)));

                return s;
            }, Map.class);

            // Parse the search results and extract the highlighted fields
            List<SearchResultVO> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();

                // Extract highlighted fields from the ES hit
                List<String> titleHighlights = hit.highlight() != null ? hit.highlight().get("title") : null;
                List<String> summaryHighlights = hit.highlight() != null ? hit.highlight().get("summary") : null;
                List<String> contentHighlights = hit.highlight() != null ? hit.highlight().get("content") : null;

                // Title: prefer the highlighted version, merging adjacent em tags
                String title = (titleHighlights != null && !titleHighlights.isEmpty())
                        ? mergeAdjacentEmTags(titleHighlights.get(0))
                        : getString(source, "title");
                String summary = (summaryHighlights != null && !summaryHighlights.isEmpty())
                        ? mergeAdjacentEmTags(summaryHighlights.get(0))
                        : getString(source, "summary");

                // Fall back to simple regex-based highlighting when ES did not return highlights
                if (titleHighlights == null && summaryHighlights == null && StringUtils.hasText(keyword)) {
                    title = highlightSimple(title, keyword);
                    summary = highlightSimple(summary, keyword);
                }

                // Content highlight fragments
                List<String> highlights;
                if (contentHighlights != null && !contentHighlights.isEmpty()) {
                    highlights = contentHighlights.stream()
                            .map(this::mergeAdjacentEmTags)
                            .collect(Collectors.toList());
                } else {
                    highlights = new ArrayList<>();
                }

                results.add(SearchResultVO.builder()
                        .id(hit.id() != null ? Long.parseLong(hit.id()) : null)
                        .title(title)
                        .summary(summary)
                        .highlights(highlights)
                        .categoryName(getString(source, "categoryName"))
                        .tagNames(getStringList(source, "tagNames"))
                        .creatorName(getString(source, "creatorName"))
                        .teamName(getString(source, "teamName"))
                        .viewCount(toInt(source != null ? source.get("viewCount") : null))
                        .likeCount(toInt(source != null ? source.get("likeCount") : null))
                        .commentCount(toInt(source != null ? source.get("commentCount") : null))
                        .publishAt(getString(source, "publishAt"))
                        .score(hit.score() != null ? hit.score().floatValue() : 0f)
                        .build());
            }

            return PageResult.<SearchResultVO>builder()
                    .records(results)
                    .total(response.hits().total() != null ? response.hits().total().value() : 0L)
                    .current((long) dto.getCurrent())
                    .size((long) dto.getSize())
                    .build();

        } catch (Exception e) {
            log.error("Keyword search failed: {}", e.getMessage(), e);
            throw new BusinessException("Search failed: " + e.getMessage());
        }
    }

    // ==================== Hybrid intelligent search ====================

    /**
     * Hybrid intelligent search (BM25 + kNN vector + RRF fusion + LLM re-ranking)
     *
     * <p>Calls kb-ai's /rag/search endpoint via Feign, obtains chunk-level retrieval results,
     * aggregates them into document-level results by documentId, and enriches them with
     * metadata from the kb_document index.</p>
     *
     * <p>Automatically falls back to keyword search on failure.</p>
     *
     * @param dto the search request
     * @return the paginated search results (including chunk details)
     */
    private PageResult<SearchResultVO> searchHybrid(SearchRequestDTO dto) {
        log.info("Hybrid intelligent search: keyword={}, topK={}, enableRerank={}",
                dto.getKeyword(), dto.getTopK(), dto.isEnableRerank());

        try {
            // Build the request and call kb-ai via Feign
            RagSearchRequest request = RagSearchRequest.builder()
                    .query(dto.getKeyword())
                    .topK(dto.getTopK() > 0 ? dto.getTopK() : 10)
                    .enableRerank(dto.isEnableRerank())
                    .build();

            Result<List<RagSearchItemVO>> result = ragSearchFeignClient.search(request);
            log.debug("kb-ai hybrid search response: code={}, items={}",
                    result.getCode(), result.getData() != null ? result.getData().size() : 0);

            List<RagSearchItemVO> dataList = result.getData();
            if (dataList == null || dataList.isEmpty()) {
                return PageResult.<SearchResultVO>builder()
                        .records(new ArrayList<>())
                        .total(0L)
                        .current((long) dto.getCurrent())
                        .size((long) dto.getSize())
                        .build();
            }

            // Aggregate chunks into document-level results by documentId
            Map<String, SearchResultVO> docMap = new LinkedHashMap<>();
            List<SearchResultVO> standaloneResults = new ArrayList<>();

            for (RagSearchItemVO item : dataList) {
                Long docId = item.getDocumentId();

                // Build the chunk result
                SearchResultVO.ChunkResult chunk = SearchResultVO.ChunkResult.builder()
                        .chunkId(item.getChunkId())
                        .content(item.getContent())
                        .heading(item.getHeading())
                        .score(item.getScore())
                        .bm25Score(item.getBm25Score())
                        .vectorScore(item.getVectorScore())
                        .build();

                if (docId == null) {
                    // No documentId: display each chunk standalone
                    String title = item.getDocumentTitle();
                    if (title == null || title.isEmpty()) {
                        title = item.getHeading();
                    }
                    SearchResultVO vo = SearchResultVO.builder()
                            .title(title != null ? title : "Untitled document")
                            .summary(chunk.getContent().length() > 200
                                    ? chunk.getContent().substring(0, 200) + "..." : chunk.getContent())
                            .score((float) chunk.getScore())
                            .bm25Score(chunk.getBm25Score())
                            .vectorScore(chunk.getVectorScore())
                            .chunks(new ArrayList<>(List.of(chunk)))
                            .build();
                    standaloneResults.add(vo);
                    continue;
                }

                // Has a documentId: aggregate chunks by docId
                String docIdStr = String.valueOf(docId);
                if (docMap.containsKey(docIdStr)) {
                    docMap.get(docIdStr).getChunks().add(chunk);
                } else {
                    String publishTime = formatPublishTime(item.getPublishTime());
                    SearchResultVO vo = SearchResultVO.builder()
                            .id(docId.longValue())
                            .title(item.getDocumentTitle())
                            .summary(chunk.getContent().length() > 200
                                    ? chunk.getContent().substring(0, 200) + "..." : chunk.getContent())
                            .publishAt(publishTime)
                            .score((float) chunk.getScore())
                            .bm25Score(chunk.getBm25Score())
                            .vectorScore(chunk.getVectorScore())
                            .chunks(new ArrayList<>(List.of(chunk)))
                            .build();
                    docMap.put(docIdStr, vo);
                }
            }

            List<SearchResultVO> records = new ArrayList<>(docMap.values());
            records.addAll(standaloneResults);

            // Apply keyword highlighting to the hybrid search results (text returned by kb-ai has no highlight markers)
            if (StringUtils.hasText(dto.getKeyword())) {
                for (SearchResultVO vo : records) {
                    vo.setTitle(highlightSimple(vo.getTitle(), dto.getKeyword()));
                    vo.setSummary(highlightSimple(vo.getSummary(), dto.getKeyword()));
                    if (vo.getChunks() != null) {
                        for (SearchResultVO.ChunkResult chunk : vo.getChunks()) {
                            chunk.setContent(highlightSimple(chunk.getContent(), dto.getKeyword()));
                        }
                    }
                }
            }

            // Enrich with document metadata (category, author, view count, etc.) from the kb_document ES index
            enrichDocumentMetadata(records);

            log.info("Hybrid intelligent search complete: {} documents ({} aggregated, {} standalone), {} chunks",
                    records.size(), docMap.size(), standaloneResults.size(), dataList.size());

            return PageResult.<SearchResultVO>builder()
                    .records(records)
                    .total((long) records.size())
                    .current((long) dto.getCurrent())
                    .size((long) dto.getSize())
                    .build();

        } catch (Exception e) {
            log.error("Hybrid intelligent search failed, falling back to keyword search: {}", e.getMessage(), e);
            return searchKeyword(dto);
        }
    }

    /**
     * Batch-enrich document metadata from the kb_document ES index
     *
     * <p>In hybrid search results, kb-ai only returns chunk-level fields (title, content, score),
     * missing metadata such as category, author, and view count. This method batch-queries
     * the ES index by document ID to fill in the gaps.</p>
     *
     * @param records the list of search results to enrich
     */
    private void enrichDocumentMetadata(List<SearchResultVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        // Collect all documents that have an ID
        List<String> ids = records.stream()
                .filter(r -> r.getId() != null)
                .map(r -> String.valueOf(r.getId()))
                .distinct()
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            return;
        }

        try {
            // Use a terms query to batch-fetch document metadata
            String idsJson = ids.stream()
                    .map(id -> "\"" + id + "\"")
                    .collect(Collectors.joining(","));
            String queryJson = "{\"query\":{\"terms\":{\"_id\":[" + idsJson + "]}},\"size\":" + ids.size() + "}";

            Query searchQuery = new StringQuery(queryJson);
            SearchHits<DocumentIndex> searchHits = elasticsearchOperations.search(searchQuery, DocumentIndex.class);

            Map<String, DocumentIndex> docIndexMap = searchHits.getSearchHits().stream()
                    .collect(Collectors.toMap(
                            SearchHit::getId,
                            SearchHit::getContent,
                            (a, b) -> a));

            // Fill in missing fields one by one
            for (SearchResultVO vo : records) {
                if (vo.getId() == null) continue;
                DocumentIndex doc = docIndexMap.get(String.valueOf(vo.getId()));
                if (doc == null) continue;

                if (vo.getSummary() == null || vo.getSummary().isEmpty()) {
                    vo.setSummary(doc.getSummary());
                }
                if (vo.getPublishAt() == null && doc.getPublishAt() != null) {
                    vo.setPublishAt(doc.getPublishAt());
                }
                if (vo.getCategoryName() == null) {
                    vo.setCategoryName(doc.getCategoryName());
                }
                if (vo.getCreatorName() == null) {
                    vo.setCreatorName(doc.getCreatorName());
                }
                if (vo.getViewCount() == null) {
                    vo.setViewCount(doc.getViewCount());
                }
                if (vo.getLikeCount() == null) {
                    vo.setLikeCount(doc.getLikeCount());
                }
                if (vo.getCommentCount() == null) {
                    vo.setCommentCount(doc.getCommentCount());
                }
                if (vo.getTagNames() == null) {
                    vo.setTagNames(doc.getTagNames());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to enrich document metadata: {}", e.getMessage());
        }
    }

    // ==================== Search suggestions ====================

    /** {@inheritDoc} */
    @Override
    public List<SearchSuggestVO> suggest(String keyword, Integer size) {
        log.info("Search suggestions: keyword={}, size={}", keyword, size);

        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }

        if (size == null || size <= 0) {
            size = 10;
        }

        try {
            // Match the title prefix using a prefix query
            String queryJson = objectMapper.writeValueAsString(
                    Map.of("prefix", Map.of("title.keyword", keyword)));
            Query searchQuery = new StringQuery(queryJson);
            searchQuery.setPageable(PageRequest.of(0, size));

            SearchHits<DocumentIndex> searchHits = elasticsearchOperations.search(searchQuery, DocumentIndex.class);

            return searchHits.getSearchHits().stream()
                    .map(hit -> SearchSuggestVO.builder()
                            .text(hit.getContent().getTitle())
                            .type("title")
                            .documentId(Long.parseLong(hit.getId()))
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Search suggestions failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // ==================== Index management ====================

    /** {@inheritDoc} */
    @Override
    public void indexDocument(Long documentId) {
        if (documentId == null) {
            return;
        }
        log.info("Indexing document: documentId={}", documentId);

        try {
            // Fetch document details from kb-document via REST
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", SYSTEM_USER_ID);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = kbDocumentBaseUrl + "/documents/" + documentId;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            String body = response.getBody();
            if (body == null) {
                log.warn("kb-document returned an empty response: documentId={}", documentId);
                return;
            }

            Map<String, Object> respMap = objectMapper.readValue(body,
                    new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> docData = (Map<String, Object>) respMap.get("data");
            if (docData == null) {
                log.warn("Document data is empty: documentId={}", documentId);
                return;
            }

            // Write to ES
            DocumentIndex docIndex = buildDocumentIndexFromMap(docData);
            elasticsearchOperations.save(docIndex);
            log.info("Document indexed successfully: documentId={}, title={}", documentId, docIndex.getTitle());

        } catch (Exception e) {
            log.error("Failed to index document: documentId={}, error={}", documentId, e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void indexDocumentData(Map<String, Object> docData) {
        try {
            DocumentIndex docIndex = buildDocumentIndexFromMap(docData);
            elasticsearchOperations.save(docIndex);
            log.info("Document data indexed successfully: id={}, title={}", docIndex.getId(), docIndex.getTitle());
        } catch (Exception e) {
            log.error("Failed to index document data: {}", e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void batchIndexDocuments(List<Long> documentIds) {
        log.info("Batch-indexing documents: documentCount={}", documentIds != null ? documentIds.size() : 0);

        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }

        for (Long documentId : documentIds) {
            try {
                indexDocument(documentId);
            } catch (Exception e) {
                log.error("Failed to batch-index document: documentId={}, error={}", documentId, e.getMessage(), e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteDocument(Long documentId) {
        log.info("Deleting document index: documentId={}", documentId);

        if (documentId == null) {
            return;
        }

        try {
            elasticsearchOperations.delete(String.valueOf(documentId), DocumentIndex.class);
            log.info("Document index deleted successfully: documentId={}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete document index: documentId={}, error={}", documentId, e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void batchDeleteDocuments(List<Long> documentIds) {
        log.info("Batch-deleting document indexes: documentCount={}", documentIds != null ? documentIds.size() : 0);

        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }

        try {
            for (Long documentId : documentIds) {
                elasticsearchOperations.delete(String.valueOf(documentId), DocumentIndex.class);
            }
            log.info("Batch document index deletion succeeded: documentCount={}", documentIds.size());
        } catch (Exception e) {
            log.error("Batch document index deletion failed: error={}", e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void rebuildIndex() {
        log.info("Starting index rebuild: synchronizing all published documents from kb-document");

        try {
            // Delete the old index
            if (esClient.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME))).value()) {
                esClient.indices().delete(DeleteIndexRequest.of(d -> d.index(INDEX_NAME)));
                log.info("Old index deleted successfully");
            }

            // Create a new index (dynamic mapping; string fields use keyword to avoid date parsing issues)
            CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
                    .index(INDEX_NAME)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("0")
                            .refreshInterval(ri -> ri.time("5s")))
                    .mappings(m -> m
                            .properties("title", p -> p.text(t -> t.fields("keyword", f -> f.keyword(k -> k))))
                            .properties("summary", p -> p.text(t -> t))
                            .properties("content", p -> p.text(t -> t))
                            .properties("categoryId", p -> p.long_(l -> l))
                            .properties("categoryName", p -> p.keyword(k -> k))
                            .properties("tagIds", p -> p.long_(l -> l))
                            .properties("tagNames", p -> p.keyword(k -> k))
                            .properties("creatorId", p -> p.long_(l -> l))
                            .properties("creatorName", p -> p.keyword(k -> k))
                            .properties("teamId", p -> p.long_(l -> l))
                            .properties("teamName", p -> p.keyword(k -> k))
                            .properties("docStatus", p -> p.integer(i -> i))
                            .properties("viewCount", p -> p.integer(i -> i))
                            .properties("likeCount", p -> p.integer(i -> i))
                            .properties("commentCount", p -> p.integer(i -> i))
                            .properties("isPublic", p -> p.boolean_(b -> b))
                            .properties("publishAt", p -> p.keyword(k -> k))
                            .properties("createdAt", p -> p.keyword(k -> k))
                            .properties("updatedAt", p -> p.keyword(k -> k))));
            esClient.indices().create(createRequest);
            log.info("New index created successfully: index={}", INDEX_NAME);

            // Page through all published documents from kb-document and index them
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", SYSTEM_USER_ID);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            long current = 1;
            long pageSize = 50;
            long totalIndexed = 0;
            boolean hasMore = true;

            while (hasMore) {
                // status=1 means published
                String pageUrl = kbDocumentBaseUrl + "/documents/page?current=" + current
                        + "&size=" + pageSize + "&status=1";
                ResponseEntity<String> response = restTemplate.exchange(pageUrl, HttpMethod.GET, entity, String.class);

                String body = response.getBody();
                if (body == null) break;

                Map<String, Object> respMap = objectMapper.readValue(body,
                        new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                Map<String, Object> pageData = (Map<String, Object>) respMap.get("data");
                if (pageData == null) break;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> records = (List<Map<String, Object>>) pageData.get("records");
                Long total = toLong(pageData.get("total"));

                if (records == null || records.isEmpty()) break;

                for (Map<String, Object> docData : records) {
                    try {
                        DocumentIndex docIndex = buildDocumentIndexFromMap(docData);
                        elasticsearchOperations.save(docIndex);
                        totalIndexed++;
                    } catch (Exception e) {
                        Long failedId = toLong(docData.get("id"));
                        log.error("Failed to index document: documentId={}, error={}", failedId, e.getMessage());
                    }
                }

                log.info("Index rebuild progress: {} indexed so far, {} total", totalIndexed, total);
                hasMore = records.size() >= pageSize;
                current++;
            }

            log.info("Index rebuild complete: {} documents in total", totalIndexed);

        } catch (Exception e) {
            log.error("Index rebuild failed: {}", e.getMessage(), e);
            throw new BusinessException("Index rebuild failed: " + e.getMessage());
        }
    }

    // ==================== Document index object construction ====================

    /**
     * Build an ES document index object from kb-document API response data
     *
     * @param docData the document data Map returned by the API
     * @return the ES index entity
     */
    private DocumentIndex buildDocumentIndexFromMap(Map<String, Object> docData) {
        Long docId = toLong(docData.get("id"));

        return DocumentIndex.builder()
                .id(docId != null ? String.valueOf(docId) : null)
                .title((String) docData.get("title"))
                .summary((String) docData.get("summary"))
                .content((String) docData.get("content"))
                .categoryId(toLong(docData.get("categoryId")))
                .categoryName((String) docData.get("categoryName"))
                .tagIds(null)
                .tagNames(parseTagNames(docData))
                .creatorId(toCreatorId(docData))
                .creatorName(toCreatorName(docData))
                .teamId(null)
                .teamName(null)
                .docStatus(toInt(docData.get("status")))
                .viewCount(toInt(docData.get("viewCount")))
                .likeCount(toInt(docData.get("likeCount")))
                .commentCount(toInt(docData.get("commentCount")))
                .isPublic(toInt(docData.get("isPublic")) == 1)
                .publishAt(formatPublishTime(getString(docData, "publishTime")))
                .createdAt(formatPublishTime(getString(docData, "createdAt")))
                .updatedAt(formatPublishTime(getString(docData, "updatedAt")))
                .build();
    }

    /**
     * Parse the tag name list (supports a comma-separated string or a JSON array)
     */
    @SuppressWarnings("unchecked")
    private List<String> parseTagNames(Map<String, Object> docData) {
        Object tags = docData.get("tags");
        if (tags instanceof String tagStr && StringUtils.hasText(tagStr)) {
            return List.of(tagStr.split(","));
        }
        if (tags instanceof List) {
            return (List<String>) tags;
        }
        return List.of();
    }

    /**
     * Get the creator ID (prefers author.id)
     */
    private Long toCreatorId(Map<String, Object> docData) {
        Object author = docData.get("author");
        if (author instanceof Map) {
            return toLong(((Map<?, ?>) author).get("id"));
        }
        return toLong(docData.get("authorId"));
    }

    /**
     * Get the creator name (prefers author.username)
     */
    private String toCreatorName(Map<String, Object> docData) {
        Object author = docData.get("author");
        if (author instanceof Map) {
            Object username = ((Map<?, ?>) author).get("username");
            return username != null ? username.toString() : null;
        }
        return (String) docData.get("authorName");
    }

    // ==================== Highlighting ====================

    /**
     * Simple keyword highlighting: wraps matching text in {@code <em>} tags (case-insensitive)
     *
     * @param text    the original text
     * @param keyword the search keyword
     * @return the highlighted HTML text
     */
    private String highlightSimple(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return text;
        }
        String escaped = Pattern.quote(keyword);
        return text.replaceAll("(?i)" + escaped, "<em>$0</em>");
    }

    /**
     * Merge adjacent em tags, resolving IK tokenizer token-fragmentation issues
     *
     * <p>For example, a two-character Chinese word split by the tokenizer into
     * {@code <em>A</em><em>B</em>} is merged back into {@code <em>AB</em>}</p>
     */
    private String mergeAdjacentEmTags(String text) {
        if (text == null) return null;
        return text.replace("</em><em>", "");
    }

    // ==================== Time formatting ====================

    /**
     * Format the publish time: {@code "2026-05-28T15:49:51"} → {@code "2026-05-28 15:49:51"}
     */
    private String formatPublishTime(String time) {
        if (time == null) return null;
        String cleaned = time.contains(".") ? time.substring(0, time.indexOf('.')) : time;
        return cleaned.replace('T', ' ');
    }

    // ==================== Type conversion utility methods ====================

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> source, String key) {
        if (source == null) return null;
        Object val = source.get(key);
        if (val instanceof List) {
            return (List<String>) val;
        }
        return null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer toInt(Object value) {
        if (value instanceof Number num) {
            return num.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private double toDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return 0.0;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number num) {
            return num.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
