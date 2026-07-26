package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chunk node property parameters
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkPropsDTO {

    /** Chunk ID */
    private String chunkId;

    /** ID of the owning document */
    private Long docId;

    /** Chunk content */
    private String content;

    /** Chunk title/section */
    private String heading;

    /** The chunk's sequence number within the document */
    private Integer chunkIndex;

    /** Total chunk count of the document */
    private Integer totalChunks;
}
