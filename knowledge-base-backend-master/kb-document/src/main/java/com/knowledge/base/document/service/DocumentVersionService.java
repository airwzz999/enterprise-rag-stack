package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.document.dto.DocumentVersionRestoreDTO;
import com.knowledge.base.document.vo.DocumentVersionVO;

import java.util.List;

/**
 * Document version management service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DocumentVersionService {

    /**
     * Creates a document version
     *
     * @param documentId    document ID
     * @param changeDescription change description
     * @param userId        user ID
     * @return whether successful
     */
    boolean createVersion(Long documentId, String changeDescription, Long userId);

    /**
     * Gets the document version list
     *
     * @param documentId document ID
     * @param current    current page
     * @param size       page size
     * @return version list
     */
    IPage<DocumentVersionVO> getVersionList(Long documentId, Long current, Long size);

    /**
     * Gets version details
     *
     * @param versionId version ID
     * @return version details
     */
    DocumentVersionVO getVersionDetail(Long versionId);

    /**
     * Restores a version
     *
     * @param documentId document ID
     * @param restoreDTO restore parameters
     * @param userId     user ID
     * @return whether successful
     */
    boolean restoreVersion(Long documentId, DocumentVersionRestoreDTO restoreDTO, Long userId);

    /**
     * Compares differences between versions
     *
     * @param versionId1 version ID 1
     * @param versionId2 version ID 2
     * @return diff content
     */
    String compareVersions(Long versionId1, Long versionId2);

    /**
     * Deletes a version
     *
     * @param versionId version ID
     * @param userId    user ID
     * @return whether successful
     */
    boolean deleteVersion(Long versionId, Long userId);
}
