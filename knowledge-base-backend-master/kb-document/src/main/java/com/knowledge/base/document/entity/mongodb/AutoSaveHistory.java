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
 * Auto-save history snapshot entity (MongoDB)
 *
 * <p>Each time an auto-save is triggered, an immutable snapshot record is written asynchronously</p>
 * <p>Used for viewing and restoring auto-save history</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "document_autosave_history")
public class AutoSaveHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MongoDB document ID (auto-generated)
     */
    @Id
    private String id;

    /**
     * Associated MySQL document ID
     */
    @Indexed
    private Long documentId;

    /**
     * Document title at snapshot time
     */
    private String title;

    /**
     * Full Markdown content
     */
    private String content;

    /**
     * Content preview (first 200 characters, used for quick preview in lists)
     */
    private String contentPreview;

    /**
     * Content length (character count)
     */
    private Integer contentLength;

    /**
     * ID of the user who triggered the auto-save
     */
    private Long authorId;

    /**
     * Snapshot save time
     */
    private LocalDateTime savedAt;

    /**
     * Whether deleted (soft-delete flag)
     */
    private Boolean deleted;
}
