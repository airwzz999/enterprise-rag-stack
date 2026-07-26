package com.knowledge.base.graph.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Graph relationship VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Graph relationship")
public class GraphRelationVO {

    @Schema(description = "Relationship ID")
    private String id;

    @Schema(description = "Source node")
    private GraphNodeVO sourceNode;

    @Schema(description = "Target node")
    private GraphNodeVO targetNode;

    @Schema(description = "Relationship type")
    private String relationType;

    @Schema(description = "Relationship weight")
    private Double weight;
}
