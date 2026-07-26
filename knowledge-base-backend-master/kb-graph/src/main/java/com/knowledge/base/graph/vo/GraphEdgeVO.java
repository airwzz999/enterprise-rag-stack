package com.knowledge.base.graph.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Graph edge VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Graph edge")
public class GraphEdgeVO {

    @Schema(description = "Edge ID")
    private String id;

    @Schema(description = "Source node ID")
    private String source;

    @Schema(description = "Target node ID")
    private String target;

    @Schema(description = "Relationship type")
    private String relation;

    @Schema(description = "Edge label")
    private String label;

    @Schema(description = "Edge weight")
    private Double weight;
}
