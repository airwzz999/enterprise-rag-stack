package com.knowledge.base.document.repository.mongodb;

import com.knowledge.base.document.entity.mongodb.DocumentContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Document content MongoDB repository
 *
 * <p>Provides CRUD operations for document content</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Repository
public interface DocumentContentRepository extends MongoRepository<DocumentContent, String> {

    /**
     * Finds content by document ID
     *
     * @param documentId document ID
     * @return document content
     */
    DocumentContent findByDocumentId(Long documentId);

    /**
     * Deletes content by document ID
     *
     * @param documentId document ID
     */
    void deleteByDocumentId(Long documentId);
}
