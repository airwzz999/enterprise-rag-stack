package com.knowledge.base.graph.entity.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.util.List;

/**
 * Knowledge entity graph node
 *
 * <p>Entities extracted from documents by the LLM, such as tech stacks, APIs, configuration items, and core concepts.
 * Maps to the Neo4j KnowledgeEntity label.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node(primaryLabel = "KnowledgeEntity")
public class KnowledgeEntityNode {

    /** Entity name (unique identifier) */
    @Id
    private String name;

    /** Entity type */
    @Property("type")
    private String type;

    /** Entity description */
    @Property("description")
    private String description;

    /** List of aliases */
    @Property("aliases")
    private List<String> aliases;

    /** Time of first occurrence */
    @Property("createdAt")
    private String createdAt;

    /** Last update time */
    @Property("updatedAt")
    private String updatedAt;

    /**
     * Entity type enum
     */
    public static final class EntityType {
        public static final String TECH_STACK = "TECH_STACK";
        public static final String API = "API";
        public static final String CONFIG = "CONFIG";
        public static final String CONCEPT = "CONCEPT";
        public static final String TOOL = "TOOL";
        public static final String PROCESS = "PROCESS";

        private EntityType() {}
    }
}
