package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Multi-hop traversal result
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraverseResultDTO {

    /** Entity name */
    private String entityName;

    /** Entity type */
    private String entityType;

    /** Entity description */
    private String description;

    /** List of relationship types along the path */
    private List<String> pathRelations;

    /** Hop count */
    private Long hops;
}
