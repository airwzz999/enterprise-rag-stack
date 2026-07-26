package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.document.dto.AutoSaveDTO;
import com.knowledge.base.document.dto.DocumentDTO;
import com.knowledge.base.document.entity.Document;
import com.knowledge.base.document.vo.DocumentNeighborVO;
import com.knowledge.base.document.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Document Service interface
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, provides document business logic operations</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DocumentService extends IService<Document> {

    /**
     * Creates a document
     *
     * @param documentDTO document information
     * @return document ID
     */
    Long createDocument(DocumentDTO documentDTO);

    /**
     * Updates a document
     *
     * @param documentDTO document information
     * @return whether successful
     */
    Boolean updateDocument(DocumentDTO documentDTO);

    /**
     * Updates the document summary (updates only the summary field, without full validation)
     *
     * @param documentId document ID
     * @param summary    summary content
     * @return whether successful
     */
    Boolean updateSummary(Long documentId, String summary);

    /**
     * Deletes a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    Boolean deleteDocument(Long documentId);

    /**
     * Queries a document by ID
     *
     * @param documentId document ID
     * @return document information
     */
    DocumentVO getDocumentById(Long documentId);

    /**
     * Views a document (increments the view count)
     *
     * @param documentId document ID
     * @return document information
     */
    DocumentVO viewDocument(Long documentId);

    /**
     * Paginated query of the document list
     *
     * @param current    current page
     * @param size       page size
     * @param categoryId category ID
     * @param keyword    search keyword
     * @param status     status
     * @param sortBy     sort field
     * @param sortOrder  sort direction
     * @return paginated document information
     */
    IPage<DocumentVO> pageDocuments(Long current, Long size, Long categoryId, Long teamId, String keyword, Integer status, String sortBy, String sortOrder, Long authorId);

    /**
     * Queries the previous and next document
     *
     * @param documentId current document ID
     * @return neighboring document information (prevId/prevTitle/nextId/nextTitle)
     */
    DocumentNeighborVO getDocumentNeighbors(Long documentId);

    /**
     * Uploads a document file
     *
     * @param file file
     * @return file path
     */
    String uploadDocumentFile(MultipartFile file);

    /**
     * Likes a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    Boolean likeDocument(Long documentId);

    /**
     * Unlikes a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    Boolean unlikeDocument(Long documentId);

    /**
     * Favorites a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    Boolean favoriteDocument(Long documentId);

    /**
     * Publishes a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    Boolean publishDocument(Long documentId);

    /**
     * Archives a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    Boolean archiveDocument(Long documentId);

    /**
     * Cleans up stale knowledge graph nodes (document graph nodes in Neo4j left over after deletion in MySQL)
     *
     * @return number of nodes cleaned up
     */
    int cleanupGraphGhostNodes();

    /**
     * Uploads a file and parses it to create a document
     *
     * @param file uploaded file
     * @return Map containing documentId / title / fileUrl / fileSize / contentLength / contentPreview
     */
    Map<String, Object> uploadAndCreateDocument(MultipartFile file);

    /**
     * Rebuilds the knowledge graph for all published documents
     *
     * @return number of documents rebuilt
     */
    int rebuildAllGraphs();

    /**
     * Auto-saves a document (creates a new draft or updates an existing draft)
     *
     * <p>Key differences from createDocument/updateDocument:
     * <ul>
     *   <li>Title is optional; an empty title is auto-filled with "Untitled Document"</li>
     *   <li>Forces the status to draft (0), does not trigger RAG/KAG/ES indexing</li>
     *   <li>Only updates non-empty fields, avoiding overwriting existing data</li>
     * </ul></p>
     *
     * @param autoSaveDTO auto-save data
     * @return document ID (returns the new ID when creating, or the existing ID when updating)
     */
    Long autoSaveDocument(AutoSaveDTO autoSaveDTO);

    /**
     * Dismisses auto-saved drafts: marks all of the current user's drafts (status=0) as acknowledged,
     * so the restore prompt no longer appears afterward.
     */
    void dismissAutoSaveDrafts();
}
