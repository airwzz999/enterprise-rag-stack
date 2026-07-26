package com.knowledge.base.ai.rag.kag.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * KAG graph retrieval context
 *
 * <p>Contains structured knowledge retrieved from the knowledge graph:
 * matched entities, reasoning paths, associated text chunks, and so on.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /** List of matched knowledge entities */
    private List<GraphEntity> matchedEntities;

    /** List of graph reasoning paths */
    private List<GraphPath> reasoningPaths;

    /** Document text chunks associated via the graph (deduplicated) */
    private List<GraphChunk> associatedChunks;

    /** Whether relevant information was found in the graph */
    @Builder.Default
    private boolean hasResults = false;

    /**
     * Graph entity (condensed form)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphEntity implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String type;
        private String description;
        private int connectionCount;
    }

    /**
     * Graph reasoning path
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphPath implements Serializable {
        private static final long serialVersionUID = 1L;
        /** List of node names along the path */
        private List<String> nodes;
        /** List of relation types along the path */
        private List<String> relations;
        /** Number of hops */
        private int hops;
    }

    /**
     * Text chunk associated via the graph
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphChunk implements Serializable {
        private static final long serialVersionUID = 1L;
        private String chunkId;
        private Long docId;
        private String docTitle;
        private String content;
        private String heading;
        /** Source entity name */
        private String entityName;
        /** Source relation path */
        private String sourcePath;
    }
}
