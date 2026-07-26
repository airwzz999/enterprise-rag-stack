package com.knowledge.base.ai.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Reindex message body
 *
 * <p>Sends index rebuild tasks over RabbitMQ.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReindexMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Task ID (UUID) */
    private String taskId;

    /** Reindex type */
    private ReindexType type;

    /** List of specified document IDs (used when type=BY_DOC_IDS) */
    private List<Long> documentIds;

    public enum ReindexType {
        ALL,                // Rebuild all published documents
        BY_DOC_IDS,         // Rebuild the specified documents
        DELETE_BY_DOC_IDS   // Delete the vector index for the specified documents
    }
}
