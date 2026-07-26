package com.knowledge.base.graph.entity.relationship;

import com.knowledge.base.graph.entity.node.KnowledgeDocumentNode;
import com.knowledge.base.graph.entity.node.DocumentChunkNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * Document-contains-chunk relationship
 *
 * <p>KnowledgeDocument -[HAS_CHUNK]-> DocumentChunk</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelationshipProperties
public class HasChunkRelation {

    @RelationshipId
    private Long id;

    /** Chunk sequence number */
    @Property("chunkIndex")
    private Integer chunkIndex;

    @TargetNode
    private DocumentChunkNode chunk;
}
