package com.knowledge.base.ai.rag.kag.retrieval;

/**
 * KAG graph retrieval service interface
 *
 * <p>Performs multi-hop reasoning retrieval based on the Neo4j knowledge graph:
 * 1. Identify entities from the user query
 * 2. Perform multi-hop traversal in Neo4j to discover associated entities and reasoning paths
 * 3. Look up associated document text chunks in reverse
 * 4. Return a structured GraphContext</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface KAGRetrievalService {

    /**
     * Retrieve structured knowledge from the knowledge graph based on query text
     *
     * @param query       the user query text
     * @param maxEntities the maximum number of matched entities
     * @param maxHops     the maximum number of hops
     * @param maxChunks   the maximum number of text chunks to return
     * @return the graph retrieval context
     */
    GraphContext retrieveGraphContext(String query, int maxEntities, int maxHops, int maxChunks);

    /**
     * Retrieval using default parameters
     *
     * @param query the user query text
     * @return the graph retrieval context
     */
    GraphContext retrieveGraphContext(String query);
}
