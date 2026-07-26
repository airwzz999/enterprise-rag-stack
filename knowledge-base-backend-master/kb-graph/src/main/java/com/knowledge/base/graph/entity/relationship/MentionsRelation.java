package com.knowledge.base.graph.entity.relationship;

import com.knowledge.base.graph.entity.node.KnowledgeEntityNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * Chunk-mentions-entity relationship
 *
 * <p>DocumentChunk -[MENTIONS]-> KnowledgeEntity</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelationshipProperties
public class MentionsRelation {

    @RelationshipId
    private Long id;

    /** Mention confidence (0.0-1.0) */
    @Property("confidence")
    private Double confidence;

    /** Source chunk ID */
    @Property("chunkId")
    private String chunkId;

    @TargetNode
    private KnowledgeEntityNode entity;
}
