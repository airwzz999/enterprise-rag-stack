package com.knowledge.base.ai.dto.kag.extraction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Extracted entity relation
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Relation type: DEPENDS_ON / USES / CONFIGURES / HAS_PART / RELATED_TO */
    public static final String DEPENDS_ON = "DEPENDS_ON";
    public static final String USES = "USES";
    public static final String CONFIGURES = "CONFIGURES";
    public static final String HAS_PART = "HAS_PART";
    public static final String RELATED_TO = "RELATED_TO";

    /** Source entity name */
    private String source;

    /** Target entity name */
    private String target;

    /** Relation type */
    private String relation;

    /** Relation weight (0.0-1.0) */
    @Builder.Default
    private Double weight = 0.8;
}
