package com.knowledge.base.ai.rag.kag.graph;

import java.util.List;

/**
 * Graph build service interface
 *
 * <p>Orchestrates the knowledge graph build pipeline: fetch documents → chunk →
 * extract entities/relations → deduplicate and merge → write to Neo4j</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface GraphBuildService {

    /**
     * Build the knowledge graph for a single document
     *
     * @param docId document ID
     * @return the number of entities built
     */
    int buildForDocument(Long docId);

    /**
     * Delete all nodes and relationships for the specified document from the graph
     *
     * @param docId document ID
     */
    void deleteForDocument(Long docId);

    /**
     * Fully rebuild the knowledge graph for all published documents
     *
     * @return the number of documents processed
     */
    int buildAll();

    /**
     * Batch-build the knowledge graph for the specified list of documents
     *
     * @param docIds list of document IDs
     * @return the number of documents processed
     */
    int buildBatch(List<Long> docIds);

    /**
     * Asynchronously publish a graph build task for a single document
     *
     * @param docId document ID
     * @return task ID
     */
    String publishBuildTask(Long docId);

    /**
     * Asynchronously publish a batch graph build task
     *
     * @param docIds list of document IDs
     * @return task ID
     */
    String publishBuildBatchTask(List<Long> docIds);

    /**
     * Asynchronously publish a full graph build task
     *
     * @return task ID
     */
    String publishBuildAllTask();

    /**
     * Asynchronously publish a graph delete task
     *
     * @param docId document ID
     * @return task ID
     */
    String publishDeleteTask(Long docId);
}
