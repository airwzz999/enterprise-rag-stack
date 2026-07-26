package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Subgraph search result
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubgraphResultDTO {

    /** List of subgraph nodes */
    private List<NodeInfo> nodes;

    /** List of subgraph relationships */
    private List<String> relationships;
}
