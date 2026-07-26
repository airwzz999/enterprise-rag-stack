package com.knowledge.base.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KAG (Knowledge Augmented Generation) configuration properties
 *
 * <p>Centrally manages all KAG-related configuration parameters, overridable
 * via application.yml or environment variables.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "kag")
public class KAGProperties {

    /** Whether the KAG feature is enabled */
    private boolean enabled = true;

    /** Entity extraction configuration */
    private Extraction extraction = new Extraction();

    /** Graph build configuration */
    private Graph graph = new Graph();

    /** Graph retrieval configuration */
    private Retrieval retrieval = new Retrieval();

    /** Hybrid fusion configuration */
    private Fusion fusion = new Fusion();

    /** Asynchronous build configuration */
    private Async async = new Async();

    @Data
    public static class Extraction {
        /** Number of text chunks processed per batch */
        private int batchSize = 5;
        /** Maximum number of entities extracted per text chunk */
        private int maxEntitiesPerChunk = 10;
        /** Maximum number of relations extracted per text chunk */
        private int maxRelationsPerChunk = 15;
        /** LLM model used for extraction */
        private String model = "qwen";
        /** Number of extraction retries */
        private int maxRetries = 2;
        /** Entity similarity merge threshold */
        private double similarityThreshold = 0.85;
    }

    @Data
    public static class Graph {
        /** Graph database name */
        private String database = "neo4j";
        /** Whether to clear old data before building */
        private boolean clearBeforeBuild = false;
        /** Batch write size */
        private int batchWriteSize = 50;
    }

    @Data
    public static class Retrieval {
        /** Maximum number of hops for graph traversal */
        private int maxHops = 2;
        /** Maximum number of nodes expanded per hop */
        private int maxEntitiesPerQuery = 10;
        /** Maximum number of text chunks associated with each entity */
        private int maxChunksPerEntity = 3;
        /** Retrieval timeout (seconds) */
        private int timeoutSeconds = 10;
    }

    @Data
    public static class Fusion {
        /** Whether LLM reranking is enabled */
        private boolean rerankEnabled = true;
        /** Fusion weight ratio between RAG and KAG results (RAG:KAG) */
        private double ragWeight = 0.5;
        private double kagWeight = 0.5;
    }

    @Data
    public static class Async {
        /** Whether asynchronous building is enabled */
        private boolean enabled = true;
        /** Number of documents processed per batch */
        private int buildBatchSize = 5;
        /** Maximum number of retries */
        private int maxRetries = 3;
    }
}
