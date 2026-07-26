package com.knowledge.base.document.service;

import com.knowledge.base.document.vo.DocumentAccessVO;

import java.util.List;

/**
 * Document access record service interface
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, provides document access record related business operations</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DocumentAccessService {

    /**
     * Records a document access
     *
     * @param userId        user ID
     * @param documentId    document ID
     * @param documentTitle document title
     */
    void recordAccess(Long userId, Long documentId, String documentTitle);

    /**
     * Gets the user's recent access records
     *
     * @param limit query result limit
     * @return access record list
     */
    List<DocumentAccessVO> getRecentAccess(Integer limit);

    /**
     * Deletes a single access record
     *
     * @param documentId document ID
     */
    void deleteAccess(Long documentId);

    /**
     * Clears all of the user's access records
     */
    void clearAllAccess();
}
