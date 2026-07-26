package com.knowledge.base.ai.rag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Document chunk POJO
 *
 * <p>Represents a single fragment of a chunked document, including a reference to
 * the source document and the chunk's position information.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /** Unique chunk ID (UUID) */
    private String chunkId;

    /** Source document ID */
    private Long documentId;

    /** Source document title */
    private String documentTitle;

    /** Chunk text content (including heading context) */
    private String content;

    /** Section heading this chunk belongs to */
    private String heading;

    /** Chunk index within the document (0-based) */
    private Integer chunkIndex;

    /** Total number of chunks for the document */
    private Integer totalChunks;

    /** Embedding vector (1024 dimensions) */
    private float[] embedding;

    /** Category ID */
    private Long categoryId;

    /** Author ID */
    private Long authorId;

    /** Team ID */
    private Long teamId;

    /** Document status */
    private Integer docStatus;

    /** Document publish time */
    private String publishTime;

    /** Index time */
    private LocalDateTime indexedAt;
}
