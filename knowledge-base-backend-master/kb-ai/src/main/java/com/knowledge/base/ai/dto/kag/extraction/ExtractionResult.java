package com.knowledge.base.ai.dto.kag.extraction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Extraction result (the complete extraction result for one document chunk)
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Source chunk ID */
    private String chunkId;

    /** Source document ID */
    private Long docId;

    /** List of extracted entities */
    private List<ExtractedEntity> entities;

    /** List of extracted relations */
    private List<ExtractedRelation> relations;

    /**
     * Whether the result is empty (no entities extracted)
     */
    public boolean isEmpty() {
        return (entities == null || entities.isEmpty()) && (relations == null || relations.isEmpty());
    }
}
