package com.knowledge.base.document.service;

import com.knowledge.base.document.entity.mongodb.DocumentContent;

/**
 * Document content service interface
 *
 * <p>Manages document content stored in MongoDB</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DocumentContentService {

    /**
     * Saves document content
     *
     * @param documentId document ID
     * @param content    document content
     * @return MongoDB document ID
     */
    String saveContent(Long documentId, String content);

    /**
     * Updates document content
     *
     * @param documentId document ID
     * @param content    new document content
     * @return whether the update succeeded
     */
    Boolean updateContent(Long documentId, String content);

    /**
     * Gets document content
     *
     * @param documentId document ID
     * @return document content
     */
    DocumentContent getContentByDocumentId(Long documentId);

    /**
     * Gets document content by MongoDB ID
     *
     * @param contentId MongoDB document ID
     * @return document content
     */
    DocumentContent getContentById(String contentId);

    /**
     * Deletes document content
     *
     * @param documentId document ID
     * @return whether the deletion succeeded
     */
    Boolean deleteContent(Long documentId);

    /**
     * Processes document content (e.g. uploading images)
     *
     * @param content original content
     * @return processed content
     */
    String processContent(String content);
}
