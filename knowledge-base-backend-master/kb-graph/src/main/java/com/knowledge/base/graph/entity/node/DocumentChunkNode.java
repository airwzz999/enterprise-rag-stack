package com.knowledge.base.graph.entity.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Document chunk graph node
 *
 * <p>Maps to the Neo4j DocumentChunk label, storing text chunks as nodes,
 * Connected to KnowledgeEntity via the MENTIONS relationship.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node(primaryLabel = "DocumentChunk")
public class DocumentChunkNode {

    /** Unique chunk ID */
    @Id
    private String chunkId;

    /** Source document ID */
    @Property("docId")
    private Long docId;

    /** Chunk text content */
    @Property("content")
    private String content;

    /** Title of the owning section */
    @Property("heading")
    private String heading;

    /** The chunk's sequence number within the document */
    @Property("chunkIndex")
    private Integer chunkIndex;

    /** Total chunk count of the document */
    @Property("totalChunks")
    private Integer totalChunks;

    /** Category ID */
    @Property("categoryId")
    private Long categoryId;

    /** Creation time */
    @Property("createdAt")
    private String createdAt;
}
