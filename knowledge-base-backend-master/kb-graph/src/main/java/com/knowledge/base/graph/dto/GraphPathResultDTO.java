package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Graph path query result
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphPathResultDTO {

    /** List of path nodes */
    private List<NodeInfo> nodes;

    /** List of path relationship types */
    private List<String> relationships;

    /** Hop count */
    private Long hops;
}
