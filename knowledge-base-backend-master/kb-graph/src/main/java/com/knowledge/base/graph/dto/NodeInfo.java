package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Graph node summary information
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeInfo {

    /** Node name */
    private String name;

    /** Node type (KnowledgeEntity / KnowledgeDocument / DocumentChunk) */
    private String type;
}
