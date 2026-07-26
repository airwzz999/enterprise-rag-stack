package com.knowledge.base.document.entity.mongodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Document content entity (MongoDB)
 *
 * <p>Stores large text content in MongoDB; MySQL only stores a reference to the MongoDB document ID</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "document_content")
public class DocumentContent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MongoDB document ID (auto-generated)
     */
    @Id
    private String id;

    /**
     * Associated MySQL document ID (used for joined queries)
     */
    @Indexed
    private Long documentId;

    /**
     * Document content (Markdown format)
     */
    private String content;

    /**
     * Content length (character count)
     */
    private Integer contentLength;

    /**
     * Content summary (HTML format, used for quick preview)
     */
    private String contentSummary;

    /**
     * Creation time
     */
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    private LocalDateTime updatedAt;

    /**
     * Version number (used for content version control)
     */
    private Integer version;

    /**
     * Whether deleted (soft-delete flag)
     */
    private Boolean deleted;
}
