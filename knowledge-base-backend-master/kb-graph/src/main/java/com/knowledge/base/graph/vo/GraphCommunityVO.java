package com.knowledge.base.graph.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Graph community VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Graph community")
public class GraphCommunityVO {

    @Schema(description = "Community ID")
    private String id;

    @Schema(description = "Community name")
    private String name;

    @Schema(description = "Number of community members")
    private Integer memberCount;

    @Schema(description = "List of community members")
    private List<GraphNodeVO> members;

    @Schema(description = "Community density")
    private Double density;
}
