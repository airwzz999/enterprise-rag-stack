package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Relationship merge parameters
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationMergeDTO {

    /** Source entity name */
    private String source;

    /** Target entity name */
    private String target;

    /** Relationship type */
    private String relationType;

    /** Relationship weight */
    private Double weight;
}
