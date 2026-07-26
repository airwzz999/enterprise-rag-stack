package com.knowledge.base.graph.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Graph node VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Graph node")
public class GraphNodeVO {

    @Schema(description = "Node ID")
    private String id;

    @Schema(description = "Node name")
    private String name;

    @Schema(description = "Node type")
    private String type;

    @Schema(description = "Node label")
    private String label;

    @Schema(description = "Node size")
    private Integer size;

    @Schema(description = "Node color")
    private String color;

    @Schema(description = "Node properties")
    private Map<String, Object> properties;

    @Schema(description = "Associated document ID (present for KnowledgeDocument / DocumentChunk)")
    private String documentId;
}
