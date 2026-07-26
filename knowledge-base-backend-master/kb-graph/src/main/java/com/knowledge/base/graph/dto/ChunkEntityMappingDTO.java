package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chunk-entity mapping parameters
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkEntityMappingDTO {

    /** Chunk ID */
    private String chunkId;

    /** Entity name */
    private String entityName;

    /** Confidence */
    private Double confidence;
}
