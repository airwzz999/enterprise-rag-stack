package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Graph statistics
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphStatsDTO {

    /** Total node count */
    private Long nodeCount;

    /** Total relationship count */
    private Long edgeCount;

    /** Entity count grouped by type */
    private List<EntityTypeCountDTO> entityTypeStats;

    /** Document node count */
    private Long documentCount;

    /** Chunk node count */
    private Long chunkCount;

    /**
     * Entity type-count statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityTypeCountDTO {

        private String type;

        private Long count;
    }
}
