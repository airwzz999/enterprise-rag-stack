package com.knowledge.base.ai.dto.kag.extraction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Extracted knowledge entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Entity name */
    private String name;

    /** Entity type (TECH_STACK / API / CONFIG / CONCEPT / TOOL / PROCESS) */
    private String type;

    /** Entity description */
    private String description;

    /** List of aliases */
    private List<String> aliases;

    /** Extraction confidence */
    @Builder.Default
    private Double confidence = 0.8;
}
