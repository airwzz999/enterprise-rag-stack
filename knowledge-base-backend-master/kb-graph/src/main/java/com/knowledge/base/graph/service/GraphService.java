package com.knowledge.base.graph.service;

import com.knowledge.base.graph.vo.*;

import java.util.List;

/**
 * Knowledge graph service interface
 *
 * <p>Provides business logic related to the knowledge graph</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface GraphService {

    /**
     * Gets the node list
     *
     * @param type the node type
     * @return the list of nodes
     */
    List<GraphNodeVO> getNodes(String type);

    /**
     * Gets the edge list
     *
     * @param sourceType the source node type
     * @param targetType the target node type
     * @return the list of edges
     */
    List<GraphEdgeVO> getEdges(String sourceType, String targetType);

    /**
     * Gets node relationships
     *
     * @param nodeId the node ID
     * @return the list of relationships
     */
    List<GraphRelationVO> getNodeRelations(String nodeId);

    /**
     * Graph search
     *
     * @param keyword the search keyword
     * @return the search results
     */
    List<GraphNodeVO> searchGraph(String keyword);

    /**
     * Path analysis
     *
     * @param sourceId the source node ID
     * @param targetId the target node ID
     * @param maxDepth the maximum depth
     * @return the list of paths
     */
    List<GraphPathVO> analyzePath(String sourceId, String targetId, Integer maxDepth);

    /**
     * Community detection
     *
     * @param algorithm the algorithm type
     * @return the list of communities
     */
    List<GraphCommunityVO> detectCommunity(String algorithm);

    /**
     * Gets the full graph data
     *
     * @param type the node type
     * @return the graph data
     */
    GraphDataVO getGraphData(String type);

    /**
     * Deletes the knowledge graph nodes for the specified document
     * <p>Deletes a KnowledgeDocument along with all its DocumentChunk child nodes and relationships, while keeping KnowledgeEntity nodes (entities may be shared across documents).</p>
     *
     * @param docId the document ID
     */
    void deleteByDocId(Long docId);

    /**
     * Cleans up orphaned graph nodes
     * <p>Deletes all KnowledgeDocument nodes whose docId is not in the given whitelist, along with their associated DocumentChunk child nodes.</p>
     *
     * @param validDocIds the list of valid document IDs (from MySQL)
     * @return the number of nodes deleted
     */
    int cleanupGhostNodes(List<Long> validDocIds);

    /**
     * Clears all graph Redis cache entries
     *
     * <p>Called by kb-ai after the knowledge graph is rebuilt, invalidating all @Cacheable caches,
     * forcing the next query to reload the latest data from Neo4j.</p>
     */
    void evictAllCaches();
}
