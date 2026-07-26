package com.knowledge.base.document.repository.mongodb;

import com.knowledge.base.document.entity.mongodb.AutoSaveHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Auto-save history MongoDB repository
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Repository
public interface AutoSaveHistoryRepository extends MongoRepository<AutoSaveHistory, String> {

    /**
     * Paginates the auto-save history for a given document (ordered by save time descending)
     *
     * @param documentId document ID
     * @param pageable   pagination parameters
     * @return paginated history records
     */
    Page<AutoSaveHistory> findByDocumentIdAndDeletedFalseOrderBySavedAtDesc(Long documentId, Pageable pageable);

    /**
     * Counts the auto-save history records for a given document
     *
     * @param documentId document ID
     * @return number of history records
     */
    long countByDocumentIdAndDeletedFalse(Long documentId);
}
