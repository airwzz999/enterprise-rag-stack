package com.knowledge.base.graph.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.graph.service.GraphService;
import com.knowledge.base.graph.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Knowledge graph controller
 *
 * <p>Designed following the Alibaba Java Development Guidelines, providing knowledge graph APIs</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@Tag(name = "Knowledge Graph", description = "Knowledge graph management API")
public class GraphController {

    @Resource
    private GraphService graphService;

    /**
     * Gets the node list
     *
     * @param type the node type
     * @return the list of nodes
     */
    @GetMapping("/nodes")
    @Operation(summary = "Get node list", description = "Get the list of knowledge graph nodes")
    public Result<List<GraphNodeVO>> getNodes(
        @Parameter(description = "Node type") @RequestParam(required = false) String type) {
        log.info("Get node list request: type={}", type);

        List<GraphNodeVO> nodes = graphService.getNodes(type);
        return Result.success(nodes);
    }

    /**
     * Gets the edge list
     *
     * @param sourceType the source node type
     * @param targetType the target node type
     * @return the list of edges
     */
    @GetMapping("/edges")
    @Operation(summary = "Get edge list", description = "Get the list of knowledge graph edges")
    public Result<List<GraphEdgeVO>> getEdges(
        @Parameter(description = "Source node type") @RequestParam(required = false) String sourceType,
        @Parameter(description = "Target node type") @RequestParam(required = false) String targetType) {
        log.info("Get edge list request: sourceType={}, targetType={}", sourceType, targetType);

        List<GraphEdgeVO> edges = graphService.getEdges(sourceType, targetType);
        return Result.success(edges);
    }

    /**
     * Gets node relationships
     *
     * @param nodeId the node ID
     * @return the list of relationships
     */
    @GetMapping("/node/{nodeId}/relations")
    @Operation(summary = "Get node relationships", description = "Get all relationships of a node")
    public Result<List<GraphRelationVO>> getNodeRelations(
        @Parameter(description = "Node ID", required = true)
        @PathVariable String nodeId) {
        log.info("Get node relationships request: nodeId={}", nodeId);

        List<GraphRelationVO> relations = graphService.getNodeRelations(nodeId);
        return Result.success(relations);
    }

    /**
     * Graph search
     *
     * @param keyword the search keyword
     * @return the search results
     */
    @GetMapping("/search")
    @Operation(summary = "Graph search", description = "Search for nodes in the knowledge graph")
    public Result<List<GraphNodeVO>> searchGraph(
        @Parameter(description = "Search keyword", required = true)
        @RequestParam String keyword) {
        log.info("Graph search request: keyword={}", keyword);

        List<GraphNodeVO> nodes = graphService.searchGraph(keyword);
        return Result.success(nodes);
    }

    /**
     * Path analysis
     *
     * @param sourceId the source node ID
     * @param targetId the target node ID
     * @param maxDepth the maximum depth
     * @return the list of paths
     */
    @GetMapping("/path")
    @Operation(summary = "Path analysis", description = "Analyze the path between two nodes")
    public Result<List<GraphPathVO>> analyzePath(
        @Parameter(description = "Source node ID", required = true)
        @RequestParam String sourceId,
        @Parameter(description = "Target node ID", required = true)
        @RequestParam String targetId,
        @Parameter(description = "Maximum depth")
        @RequestParam(defaultValue = "5") Integer maxDepth) {
        log.info("Path analysis request: sourceId={}, targetId={}, maxDepth={}", sourceId, targetId, maxDepth);

        List<GraphPathVO> paths = graphService.analyzePath(sourceId, targetId, maxDepth);
        return Result.success(paths);
    }

    /**
     * Community detection
     *
     * @param algorithm the algorithm type
     * @return the list of communities
     */
    @GetMapping("/community")
    @Operation(summary = "Community detection", description = "Detect community structures in the graph")
    public Result<List<GraphCommunityVO>> detectCommunity(
        @Parameter(description = "Algorithm type")
        @RequestParam(defaultValue = "label_propagation") String algorithm) {
        log.info("Community detection request: algorithm={}", algorithm);

        List<GraphCommunityVO> communities = graphService.detectCommunity(algorithm);
        return Result.success(communities);
    }

    /**
     * Gets the full graph data
     *
     * @param type the node type
     * @return the graph data
     */
    @GetMapping("/data")
    @Operation(summary = "Get full graph data", description = "Get the complete knowledge graph data")
    public Result<GraphDataVO> getGraphData(
        @Parameter(description = "Node type") @RequestParam(required = false) String type) {
        log.info("Get full graph data request: type={}", type);

        GraphDataVO graphData = graphService.getGraphData(type);
        return Result.success(graphData);
    }

    /**
     * Deletes a document's graph data
     *
     * <p>Deletes the KnowledgeDocument node corresponding to a document along with its DocumentChunk child nodes, while keeping KnowledgeEntity nodes shared across documents.</p>
     *
     * @param docId the document ID
     * @return the operation result
     */
    @DeleteMapping("/document/{docId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete document graph", description = "Delete the knowledge graph nodes and relationships for the specified document")
    public Result<String> deleteDocumentGraph(
            @Parameter(description = "Document ID", required = true) @PathVariable Long docId) {
        log.info("Delete document graph request: docId={}", docId);
        graphService.deleteByDocId(docId);
        return Result.success("Document graph data deleted");
    }

    /**
     * Cleans up orphaned graph nodes
     *
     * <p>Accepts a whitelist of valid document IDs and deletes KnowledgeDocument nodes in Neo4j whose docId is not in the whitelist, along with their associated child nodes.</p>
     *
     * @param body the request body containing validDocIds
     * @return the cleanup result
     */
    @PostMapping("/document/cleanup")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Clean up orphaned graph nodes", description = "Delete graph nodes corresponding to documents that no longer exist in MySQL")
    public Result<String> cleanupGhostNodes(@RequestBody Map<String, List<Long>> body) {
        List<Long> validDocIds = body.get("validDocIds");
        log.info("Clean up orphaned graph nodes request: validDocIds.size={}", validDocIds != null ? validDocIds.size() : 0);
        int deleted = graphService.cleanupGhostNodes(validDocIds);
        return Result.success("Cleaned up " + deleted + " orphaned graph nodes");
    }

    /**
     * Clears the graph Redis cache
     *
     * <p>Called by kb-ai after knowledge graph rebuilding completes, invalidating all graph query caches,
     * ensuring the frontend gets the latest data from Neo4j on the next query.</p>
     *
     * @return the operation result
     */
    @PostMapping("/cache/evict")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Clear graph cache", description = "Clear all knowledge graph Redis cache entries, forcing a reload from Neo4j")
    public Result<String> evictCache() {
        log.info("Clear graph cache request");
        graphService.evictAllCaches();
        return Result.success("Graph cache cleared");
    }
}
