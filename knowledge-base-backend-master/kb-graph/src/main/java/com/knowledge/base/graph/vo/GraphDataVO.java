package com.knowledge.base.graph.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Graph data VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Graph data")
public class GraphDataVO {

    @Schema(description = "List of nodes")
    private List<GraphNodeVO> nodes;

    @Schema(description = "List of edges")
    private List<GraphEdgeVO> edges;

    @Schema(description = "Total nodes")
    private Integer nodeCount;

    @Schema(description = "Total edges")
    private Integer edgeCount;
}
