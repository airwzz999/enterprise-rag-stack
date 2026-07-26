package com.knowledge.base.graph.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Graph path VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Graph path")
public class GraphPathVO {

    @Schema(description = "List of path nodes")
    private List<GraphNodeVO> nodes;

    @Schema(description = "List of path edges")
    private List<GraphEdgeVO> edges;

    @Schema(description = "Path length")
    private Integer length;

    @Schema(description = "Path weight")
    private Double weight;
}
