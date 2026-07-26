package com.knowledge.base.ai.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * KAG graph build message
 *
 * <p>Asynchronously carries graph build tasks over RabbitMQ.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KAGReindexMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Task ID (UUID) */
    private String taskId;

    /** Task type */
    private KAGBuildType type;

    /** List of document IDs */
    private List<Long> documentIds;

    /**
     * KAG build type
     */
    public enum KAGBuildType {
        /** Fully build the knowledge graph for all published documents */
        BUILD_ALL,
        /** Build the knowledge graph for the specified documents */
        BUILD_BY_DOC_IDS,
        /** Delete the knowledge graph for the specified documents */
        DELETE_BY_DOC_IDS
    }
}
