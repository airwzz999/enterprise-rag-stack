package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.document.dto.AutoSaveHistoryQueryDTO;
import com.knowledge.base.document.vo.AutoSaveHistoryVO;

/**
 * Auto-save history service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface AutoSaveHistoryService {

    /**
     * Saves an auto-save snapshot to MongoDB
     *
     * @param documentId document ID
     * @param title      document title
     * @param content    document content
     * @param authorId   user ID
     */
    void saveSnapshot(Long documentId, String title, String content, Long authorId);

    /**
     * Paginated query of the auto-save history for a given document
     *
     * @param query query parameters
     * @return paginated history records
     */
    IPage<AutoSaveHistoryVO> pageHistory(AutoSaveHistoryQueryDTO query);

    /**
     * Gets a single snapshot's details (including full content)
     *
     * @param snapshotId snapshot ID
     * @param documentId document ID
     * @return snapshot details VO
     */
    AutoSaveHistoryVO getSnapshot(String snapshotId, Long documentId);

    /**
     * Soft-deletes all auto-save history for a given document
     *
     * @param documentId document ID
     */
    void deleteByDocumentId(Long documentId);
}
