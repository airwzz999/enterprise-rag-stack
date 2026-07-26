package com.knowledge.base.document.service;

import com.knowledge.base.document.dto.ShareDTO;
import com.knowledge.base.document.vo.ShareVO;

import java.util.List;

/**
 * Document share service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DocumentShareService {

    /**
     * Creates a share link
     *
     * @param shareDTO share parameters
     * @return share information
     */
    ShareVO createShare(ShareDTO shareDTO);

    /**
     * Gets share information by share ID
     *
     * @param shareId share ID
     * @return share information
     */
    ShareVO getShareById(String shareId);

    /**
     * Verifies share access permission
     *
     * @param shareId share ID
     * @param password access password (optional)
     * @return whether access is authorized
     */
    boolean verifyShareAccess(String shareId, String password);

    /**
     * Accesses a share link
     *
     * @param shareId share ID
     * @param password access password (optional)
     * @return document ID
     */
    Long accessShare(String shareId, String password);

    /**
     * Gets all share links for a document
     *
     * @param documentId document ID
     * @return share list
     */
    List<ShareVO> getSharesByDocumentId(Long documentId);

    /**
     * Gets the current user's share list
     *
     * @return share list
     */
    List<ShareVO> getMyShares();

    /**
     * Deletes a share link
     *
     * @param shareId share ID
     * @return whether successful
     */
    boolean deleteShare(String shareId);

    /**
     * Batch-deletes share links
     *
     * @param shareIds share ID list
     * @return delete count
     */
    int batchDeleteShares(List<String> shareIds);

    /**
     * Updates share settings
     *
     * @param shareId share ID
     * @param shareDTO update parameters
     * @return whether successful
     */
    boolean updateShare(String shareId, ShareDTO shareDTO);
}
