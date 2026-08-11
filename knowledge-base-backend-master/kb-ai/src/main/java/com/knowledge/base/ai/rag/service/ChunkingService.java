package com.knowledge.base.ai.rag.service;

import com.knowledge.base.ai.rag.entity.DocumentChunk;

import java.util.List;

/**
 * Document chunking service interface
 *
 * <p>Splits long documents into small chunks suitable for embedding and retrieval,
 * according to a chunking strategy.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface ChunkingService {

    /**
     * Chunk a Markdown document
     *
     * @param content       the document content in Markdown format
     * @param documentId    document ID
     * @param documentTitle document title
     * @param categoryId    category ID
     * @param authorId      author ID
     * @param teamId        team ID
     * @param docStatus     document status
     * @param isPublic      whether the document is public (0-private, 1-public)
     * @param publishTime   document publish time
     * @return the list of chunks
     */
    List<DocumentChunk> chunk(String content, Long documentId, String documentTitle,
                              Long categoryId, Long authorId, Long teamId, Integer docStatus,
                              Integer isPublic, String publishTime);
}
