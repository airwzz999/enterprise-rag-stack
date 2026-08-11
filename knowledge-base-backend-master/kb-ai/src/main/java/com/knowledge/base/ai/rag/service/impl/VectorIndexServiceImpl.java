package com.knowledge.base.ai.rag.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson2.JSON;
import com.knowledge.base.ai.config.RagProperties;
import com.knowledge.base.ai.rag.entity.DocumentChunk;
import com.knowledge.base.ai.rag.service.VectorIndexService;
import com.knowledge.base.ai.vo.RagSearchResultVO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Vector index service implementation
 *
 * <p>Uses Elasticsearch as the vector store, implementing:
 * <ol>
 *   <li><b>Index writes</b>: bulk writes via the low-level ElasticsearchClient (supports the dense_vector field)</li>
 *   <li><b>BM25 search</b>: match queries via the low-level ElasticsearchClient (ik_max_word tokenization)</li>
 *   <li><b>kNN search</b>: knn queries via ElasticsearchClient (cosine similarity)</li>
 *   <li><b>RRF fusion</b>: reciprocal rank fusion, C=60</li>
 * </ol>
 * Both BM25 and kNN search are scoped to documents the requesting user can actually see
 * (published + public, or published + authored by them) via a shared access filter - see
 * {@link #buildAccessFilter(Long)}.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorIndexServiceImpl implements VectorIndexService {

    private final ElasticsearchClient esClient;
    private final RagProperties ragProperties;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    private static final String INDEX_NAME = "kb_chunk";

    /** {@inheritDoc} */
    @Override
    public void indexChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        createIndexIfNotExists();

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (DocumentChunk chunk : chunks) {
            Map<String, Object> docMap = buildDocMap(chunk);
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .index(INDEX_NAME)
                            .id(chunk.getChunkId())
                            .document(docMap)));
        }

        try {
            BulkResponse response = esClient.bulk(bulkBuilder.build());
            if (response.errors()) {
                List<String> failedIds = response.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.id() + ": " + item.error().reason())
                        .collect(Collectors.toList());
                log.error("ES bulk indexing partially failed: {}", String.join(", ", failedIds));
            } else {
                log.info("ES bulk indexing succeeded: {} chunks", chunks.size());
            }
        } catch (Exception e) {
            log.error("ES bulk indexing failed: {}", e.getMessage(), e);
            throw new RuntimeException("ES bulk indexing failed: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteByDocId(Long documentId) {
        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("document_id").value(documentId))));
            esClient.deleteByQuery(request);
            log.info("Deleted document chunks from ES: documentId={}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete document chunks from ES: documentId={}, error={}", documentId, e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<RagSearchResultVO> searchHybrid(String queryText, float[] queryEmbedding,
                                                 int topK, int hybridTopK, int rrfC, Long userId) {
        // Run BM25 + kNN in parallel (BM25 only if the embedding is null)
        CompletableFuture<List<SearchResult>> bm25Future = CompletableFuture.supplyAsync(() ->
                bm25Search(queryText, hybridTopK, userId), asyncTaskExecutor);

        CompletableFuture<List<SearchResult>> knnFuture;
        if (queryEmbedding != null) {
            knnFuture = CompletableFuture.supplyAsync(() ->
                    knnSearch(queryEmbedding, hybridTopK, userId), asyncTaskExecutor);
        } else {
            knnFuture = CompletableFuture.completedFuture(List.of());
        }

        List<SearchResult> bm25Results;
        List<SearchResult> knnResults;
        try {
            bm25Results = bm25Future.get(30, TimeUnit.SECONDS);
            knnResults = knnFuture.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Hybrid search timed out or failed: {}", e.getMessage());
            bm25Results = bm25Future.getNow(List.of());
            knnResults = knnFuture.getNow(List.of());
        }

        log.debug("Hybrid search results: BM25={}, kNN={}", bm25Results.size(), knnResults.size());

        // RRF fusion
        Map<String, SearchResult> fused = rrfFuse(bm25Results, knnResults, rrfC);

        // Sort by fused score descending, take topK
        return fused.values().stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .limit(topK)
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public boolean indexExists() {
        try {
            return esClient.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME))).value();
        } catch (Exception e) {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void createIndexIfNotExists() {
        try {
            if (indexExists()) {
                // Ensure new field mappings exist (e.g. publish_time, is_public)
                ensurePublishTimeMapping();
                ensureIsPublicMapping();
                return;
            }
            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                    .index(INDEX_NAME)
                    .settings(s -> s
                            .numberOfShards("3")
                            .numberOfReplicas("1")
                            .refreshInterval(ri -> ri.time("5s")))
                    .mappings(m -> m
                            .properties("chunk_id", p -> p.keyword(k -> k))
                            .properties("document_id", p -> p.long_(l -> l))
                            .properties("document_title", p -> p.text(t -> t
                                    .fields("keyword", f -> f.keyword(k -> k))))
                            .properties("content", p -> p.text(t -> t))
                            .properties("heading", p -> p.keyword(k -> k))
                            .properties("chunk_index", p -> p.integer(i -> i))
                            .properties("total_chunks", p -> p.integer(i -> i))
                            .properties("category_id", p -> p.long_(l -> l))
                            .properties("author_id", p -> p.long_(l -> l))
                            .properties("team_id", p -> p.long_(l -> l))
                            .properties("doc_status", p -> p.integer(i -> i))
                            .properties("is_public", p -> p.integer(i -> i))
                            .properties("publish_time", p -> p.date(d -> d))
                            .properties("indexed_at", p -> p.date(d -> d))
                            .properties("embedding", p -> p.denseVector(dv -> dv
                                    .dims(ragProperties.getEmbedding().getDimension())
                                    .index(true)
                                    .similarity("cosine")))));
            esClient.indices().create(request);
            log.info("ES index created successfully: index={}", INDEX_NAME);
        } catch (Exception e) {
            // Ignore if the index already exists (concurrency scenario or inaccurate exists check)
            if (e.getMessage() != null && e.getMessage().contains("resource_already_exists_exception")) {
                log.info("ES index already exists, skipping creation: index={}", INDEX_NAME);
                return;
            }
            log.error("Failed to create ES index: {}", e.getMessage(), e);
        }
    }

    /**
     * Ensure the publish_time field mapping exists (for compatibility with older index upgrades)
     */
    private void ensurePublishTimeMapping() {
        try {
            esClient.indices().putMapping(m -> m
                    .index(INDEX_NAME)
                    .properties("publish_time", p -> p.date(d -> d)));
            log.debug("publish_time field mapping updated");
        } catch (Exception e) {
            log.debug("Updating publish_time mapping: {}", e.getMessage());
        }
    }

    /**
     * Ensure the is_public field mapping exists (for compatibility with older index upgrades).
     * Chunks indexed before this field was introduced have no is_public value, so the access
     * filter in bm25Search/knnSearch treats a missing value as private (author-only) - they
     * won't surface for other users until reindexed, rather than defaulting to publicly visible.
     */
    private void ensureIsPublicMapping() {
        try {
            esClient.indices().putMapping(m -> m
                    .index(INDEX_NAME)
                    .properties("is_public", p -> p.integer(i -> i)));
            log.debug("is_public field mapping updated");
        } catch (Exception e) {
            log.debug("Updating is_public mapping: {}", e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void dropIndex() {
        try {
            esClient.indices().delete(DeleteIndexRequest.of(d -> d.index(INDEX_NAME)));
            log.info("ES index deleted: index={}", INDEX_NAME);
        } catch (Exception e) {
            log.warn("Failed to delete ES index: {}", e.getMessage());
        }
    }

    // ==================== Private Methods ====================

    private Map<String, Object> buildDocMap(DocumentChunk chunk) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("chunk_id", chunk.getChunkId());
        doc.put("document_id", chunk.getDocumentId());
        doc.put("document_title", chunk.getDocumentTitle());
        doc.put("content", chunk.getContent());
        doc.put("heading", chunk.getHeading());
        doc.put("chunk_index", chunk.getChunkIndex());
        doc.put("total_chunks", chunk.getTotalChunks());
        doc.put("category_id", chunk.getCategoryId());
        doc.put("author_id", chunk.getAuthorId());
        doc.put("team_id", chunk.getTeamId());
        doc.put("doc_status", chunk.getDocStatus());
        doc.put("is_public", chunk.getIsPublic());
        doc.put("publish_time", chunk.getPublishTime());
        doc.put("indexed_at", LocalDateTime.now().toString());
        if (chunk.getEmbedding() != null) {
            doc.put("embedding", chunk.getEmbedding());
        }
        return doc;
    }

    /**
     * Builds the access-control filter shared by BM25 and kNN search: only chunks belonging
     * to a published document that is either public or authored by the requesting user.
     *
     * <p>Team-shared (non-public, non-own) documents are excluded rather than treated as
     * visible - this service has no way to verify team membership, so failing open there
     * would just move the same access-control hole to a different boundary. Chunks indexed
     * before the is_public field existed have no value for it, so they fall on the private
     * side of this filter (author-only) until reindexed.</p>
     */
    private Query buildAccessFilter(Long userId) {
        Query statusFilter = Query.of(q -> q.term(t -> t.field("doc_status").value(1)));

        Query visibilityFilter = userId != null
                ? Query.of(q -> q.bool(b -> b
                        .should(s -> s.term(t -> t.field("is_public").value(1)))
                        .should(s -> s.term(t -> t.field("author_id").value(userId)))
                        .minimumShouldMatch("1")))
                : Query.of(q -> q.term(t -> t.field("is_public").value(1)));

        return Query.of(q -> q.bool(b -> b.filter(statusFilter).filter(visibilityFilter)));
    }

    /**
     * BM25 keyword search
     */
    private List<SearchResult> bm25Search(String queryText, int topK, Long userId) {
        try {
            Query textQuery = Query.of(q -> q.bool(b -> b
                    .should(s -> s.match(m -> m.field("content").query(queryText)))
                    .should(s -> s.match(m -> m.field("document_title").query(queryText)))
                    .minimumShouldMatch("1")));

            Query finalQuery = Query.of(q -> q.bool(b -> b
                    .must(textQuery)
                    .filter(buildAccessFilter(userId))));

            SearchRequest request = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(finalQuery)
                    .size(topK));

            SearchResponse<Map> response = esClient.search(request, Map.class);

            List<SearchResult> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) continue;
                SearchResult sr = SearchResult.of(
                        (String) source.get("chunk_id"),
                        toLong(source.get("document_id")),
                        (String) source.get("document_title"),
                        (String) source.get("content"),
                        (String) source.get("heading"),
                        hit.score() != null ? hit.score().floatValue() : 0.0);
                sr.publishTime = (String) source.get("publish_time");
                results.add(sr);
            }
            return results;
        } catch (Exception e) {
            log.warn("BM25 search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * kNN vector search
     *
     * <p>Uses a script_score query to implement cosine similarity retrieval (compatible with ES 7.x)</p>
     */
    private List<SearchResult> knnSearch(float[] queryEmbedding, int topK, Long userId) {
        try {
            Query scriptScoreQuery = Query.of(q -> q
                    .scriptScore(ss -> ss
                            .query(buildAccessFilter(userId))
                            .script(s -> s.inline(i -> i
                                    .source("cosineSimilarity(params.query_vector, 'embedding') + 1.0")
                                    .params("query_vector", JsonData.of(toFloatList(queryEmbedding)))))));

            SearchRequest request = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(scriptScoreQuery)
                    .size(topK));

            SearchResponse<Map> response = esClient.search(request, Map.class);

            List<SearchResult> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) continue;
                SearchResult sr = SearchResult.of(
                        (String) source.get("chunk_id"),
                        toLong(source.get("document_id")),
                        (String) source.get("document_title"),
                        (String) source.get("content"),
                        (String) source.get("heading"),
                        hit.score() != null ? hit.score().floatValue() : 0.0);
                sr.publishTime = (String) source.get("publish_time");
                results.add(sr);
            }
            return results;
        } catch (Exception e) {
            log.warn("kNN search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Reciprocal Rank Fusion (RRF)
     * score(d) = SUM_{r in {bm25, knn}} 1 / (C + rank_r(d))
     */
    private Map<String, SearchResult> rrfFuse(List<SearchResult> bm25Results,
                                               List<SearchResult> knnResults, int c) {
        Map<String, SearchResult> fused = new ConcurrentHashMap<>();

        // Rank by score descending
        List<SearchResult> sortedBm25 = bm25Results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .collect(Collectors.toList());
        List<SearchResult> sortedKnn = knnResults.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .collect(Collectors.toList());

        // BM25 contribution
        for (int rank = 0; rank < sortedBm25.size(); rank++) {
            SearchResult sr = sortedBm25.get(rank);
            double originalScore = sr.getScore(); // Save the original score
            double rrfScore = 1.0 / (c + rank + 1);
            SearchResult existing = fused.get(sr.chunkId);
            if (existing == null) {
                sr.score = rrfScore;
                sr.bm25Score = originalScore;
                sr.vectorScore = 0;
                fused.put(sr.chunkId, sr);
            } else {
                existing.score += rrfScore;
                existing.bm25Score = originalScore;
            }
        }

        // kNN contribution
        for (int rank = 0; rank < sortedKnn.size(); rank++) {
            SearchResult sr = sortedKnn.get(rank);
            double originalScore = sr.getScore(); // Save the original score
            double rrfScore = 1.0 / (c + rank + 1);
            SearchResult existing = fused.get(sr.chunkId);
            if (existing == null) {
                sr.score = rrfScore;
                sr.bm25Score = 0;
                sr.vectorScore = originalScore;
                fused.put(sr.chunkId, sr);
            } else {
                existing.score += rrfScore;
                existing.vectorScore = originalScore;
            }
        }

        return fused;
    }

    private RagSearchResultVO toVO(SearchResult sr) {
        return RagSearchResultVO.builder()
                .chunkId(sr.chunkId)
                .documentId(sr.documentId)
                .documentTitle(sr.documentTitle)
                .content(sr.content)
                .heading(sr.heading)
                .publishTime(sr.publishTime)
                .score(sr.score)
                .bm25Score(sr.bm25Score)
                .vectorScore(sr.vectorScore)
                .build();
    }

    private static List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    /**
     * Search result inner class (used for RRF fusion calculations)
     */
    private static class SearchResult {
        String chunkId;
        Long documentId;
        String documentTitle;
        String content;
        String heading;
        double score;
        double bm25Score;
        double vectorScore;
        String publishTime;

        static SearchResult of(String chunkId, Long documentId, String documentTitle,
                               String content, String heading, double score) {
            SearchResult sr = new SearchResult();
            sr.chunkId = chunkId;
            sr.documentId = documentId;
            sr.documentTitle = documentTitle;
            sr.content = content;
            sr.heading = heading;
            sr.score = score;
            return sr;
        }

        public double getScore() { return score; }
    }
}
