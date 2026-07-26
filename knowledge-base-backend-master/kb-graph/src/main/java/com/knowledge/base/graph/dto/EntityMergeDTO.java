package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Entity merge parameters
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityMergeDTO {

    /** Entity name */
    private String name;

    /** Entity type */
    private String type;

    /** Entity description */
    private String description;

    /** List of aliases */
    private List<String> aliases;
}
