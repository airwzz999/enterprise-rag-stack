package com.knowledge.base.graph.repository;

import com.knowledge.base.graph.dto.*;

import java.util.List;

/**
 * Custom graph query repository
 *
 * <p>Uses Neo4jClient to execute complex Cypher queries:
 * multi-hop path analysis, community detection, graph data export, and more.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface CustomGraphRepository {

    /**
     * Queries all shortest paths between two entities
     *
     * @param source   the source entity name
     * @param target   the target entity name
     * @param maxDepth the maximum depth
     * @return the list of paths
     */
    List<GraphPathResultDTO> findShortestPaths(String source, String target, int maxDepth);

    /**
     * Performs a multi-hop traversal starting from an entity
     *
     * @param entityName the starting entity name
     * @param maxHops    the maximum number of hops
     * @param limit      the limit on the number of nodes returned
     * @return the list of traversed entities (including relationships)
     */
    List<TraverseResultDTO> traverseFromEntity(String entityName, int maxHops, int limit);

    /**
     * Gets the graph statistics
     *
     * @return the graph statistics
     */
    GraphStatsDTO getGraphStatistics();

    /**
     * Community detection (simplified version: based on connected components)
     *
     * @param minCommunitySize the minimum number of relationships for a community
     * @return the list of community members
     */
    List<CommunityMemberDTO> detectCommunities(int minCommunitySize);

    /**
     * Searches by keyword and returns a subgraph
     *
     * @param keyword  the keyword
     * @param maxNodes the maximum number of nodes
     * @return the subgraph data
     */
    SubgraphResultDTO searchSubgraph(String keyword, int maxNodes);

    /**
     * Batch-MERGEs entity nodes
     *
     * @param entities the list of entity properties
     */
    void mergeEntities(List<EntityMergeDTO> entities);

    /**
     * Batch-MERGEs entity relationships
     *
     * @param relations the list of relationships
     */
    void mergeRelations(List<RelationMergeDTO> relations);

    /**
     * Connects a chunk with an entity (MENTIONS relationship)
     *
     * @param chunkEntityMappings the list of mappings
     */
    void connectChunksToEntities(List<ChunkEntityMappingDTO> chunkEntityMappings);

    /**
     * Creates a document node (including category information)
     */
    void createDocumentNode(DocumentPropsDTO docProps);

    /**
     * Creates a chunk node
     */
    void createChunkNode(ChunkPropsDTO chunkProps);

    /**
     * Creates a HAS_CHUNK relationship from a document to a chunk
     */
    void createHasChunkRelation(Long docId, String chunkId, int chunkIndex);
}
