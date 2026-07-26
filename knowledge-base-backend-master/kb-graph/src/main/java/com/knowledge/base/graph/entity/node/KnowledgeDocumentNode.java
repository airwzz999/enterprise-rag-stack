package com.knowledge.base.graph.entity.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Knowledge document graph node
 *
 * <p>Maps to the Neo4j KnowledgeDocument label, storing document metadata.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node(primaryLabel = "KnowledgeDocument")
public class KnowledgeDocumentNode {

    /** Document ID (from kb-document MySQL) */
    @Id
    private Long docId;

    /** Document title */
    @Property("title")
    private String title;

    /** Document summary */
    @Property("summary")
    private String summary;

    /** Category ID */
    @Property("categoryId")
    private Long categoryId;

    /** Author ID */
    @Property("authorId")
    private Long authorId;

    /** Author name */
    @Property("authorName")
    private String authorName;

    /** Document status: 0-draft, 1-published, 2-archived */
    @Property("status")
    private Integer status;

    /** Publish time */
    @Property("publishTime")
    private String publishTime;

    /** Document type: 1-article, 2-file */
    @Property("documentType")
    private Integer documentType;

    /** Tags (comma-separated) */
    @Property("tags")
    private String tags;

    /** Creation time */
    @Property("createdAt")
    private String createdAt;

    /** Update time */
    @Property("updatedAt")
    private String updatedAt;
}
