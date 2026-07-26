package com.knowledge.base.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG configuration properties
 *
 * <p>Centrally manages all RAG-related configuration parameters, overridable
 * via application.yml or environment variables.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /** Whether the RAG feature is enabled */
    private boolean enabled = true;

    /** Chunking configuration */
    private Chunking chunking = new Chunking();

    /** Embedding configuration */
    private Embedding embedding = new Embedding();

    /** Retrieval configuration */
    private Retrieval retrieval = new Retrieval();

    /** Reranking configuration */
    private Rerank rerank = new Rerank();

    /** Index configuration */
    private Index index = new Index();

    /** Asynchronous configuration */
    private Async async = new Async();

    @Data
    public static class Chunking {
        /** Target token count per chunk */
        private int chunkSize = 512;
        /** Overlapping token count between chunks */
        private int chunkOverlap = 64;
        /** Whether paragraph-aware chunking is enabled */
        private boolean paragraphAware = true;
    }

    @Data
    public static class Embedding {
        /** Embedding model name */
        private String model = "text-embedding-v3";
        /** Embedding vector dimension */
        private int dimension = 1024;
        /** Embedding provider: qwen */
        private String provider = "qwen";
        /** Batch embedding size */
        private int batchSize = 20;
        /** Whether embedding results are cached */
        private boolean cacheEnabled = true;
        /** Embedding cache TTL (seconds) */
        private long cacheTtlSeconds = 86400;
    }

    @Data
    public static class Retrieval {
        /** Default Top-K to return */
        private int defaultTopK = 5;
        /** Hybrid retrieval Top-K (number returned by each of BM25 and kNN) */
        private int hybridTopK = 20;
        /** Final Top-K to return */
        private int finalTopK = 5;
        /** RRF (Reciprocal Rank Fusion) constant */
        private int rrfC = 60;
    }

    @Data
    public static class Rerank {
        /** Whether reranking is enabled */
        private boolean enabled = true;
        /** Model used for reranking */
        private String model = "qwen";
    }

    @Data
    public static class Index {
        /** ES chunk index name */
        private String chunkIndexName = "kb_chunk";
    }

    @Data
    public static class Async {
        /** Whether asynchronous indexing is enabled */
        private boolean enabled = true;
        /** Number of documents processed per batch */
        private int reindexBatchSize = 10;
    }
}
