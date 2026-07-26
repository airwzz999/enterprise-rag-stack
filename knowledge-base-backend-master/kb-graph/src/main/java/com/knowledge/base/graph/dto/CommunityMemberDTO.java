package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Community member (used in community detection results)
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityMemberDTO {

    /** Entity name */
    private String name;

    /** Entity type */
    private String type;

    /** Connection degree */
    private Long degree;
}
