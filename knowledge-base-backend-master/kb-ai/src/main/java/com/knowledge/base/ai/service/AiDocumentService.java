package com.knowledge.base.ai.service;

import com.knowledge.base.ai.dto.DocumentProcessDTO;
import com.knowledge.base.ai.vo.DocumentProcessVO;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI document processing service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface AiDocumentService {

    /**
     * Generate a document summary
     *
     * @param content document content
     * @param length  summary length
     * @return the summary content
     */
    String generateSummary(String content, Integer length);

    /**
     * Generate a document outline
     *
     * @param content document content
     * @param level   outline level
     * @return the outline content
     */
    String generateOutline(String content, Integer level);

    /**
     * Expand content
     *
     * @param content  document content
     * @param expType  expansion type
     * @return the expanded content
     */
    String expandContent(String content, String expType);

    /**
     * Improve wording
     *
     * @param content  document content
     * @param target   optimization target
     * @return the improved content
     */
    String optimizeContent(String content, String target);

    /**
     * Add examples
     *
     * @param content  document content
     * @param expType  example type
     * @return the content with examples added
     */
    String addExample(String content, String expType);

    /**
     * Process a document (unified entry point)
     *
     * @param processDTO processing request
     * @return the processing result
     */
    DocumentProcessVO processDocument(DocumentProcessDTO processDTO);

    /**
     * Generate a document summary (file upload variant)
     *
     * @param file   document file
     * @param userId user ID
     * @return the processing result
     */
    DocumentProcessVO generateSummary(MultipartFile file, Long userId);

    /**
     * Generate a document outline (file upload variant)
     *
     * @param file   document file
     * @param userId user ID
     * @return the processing result
     */
    DocumentProcessVO generateOutline(MultipartFile file, Long userId);

    /**
     * Expand content (DTO variant)
     *
     * @param dto    processing request
     * @param userId user ID
     * @return the processing result
     */
    DocumentProcessVO expandContent(DocumentProcessDTO dto, Long userId);

    /**
     * Improve wording (DTO variant)
     *
     * @param dto    processing request
     * @param userId user ID
     * @return the processing result
     */
    DocumentProcessVO optimizeContent(DocumentProcessDTO dto, Long userId);

    /**
     * Generate a summary in streaming mode
     *
     * @param file   document file
     * @param userId user ID
     * @return SSE event stream
     */
    SseEmitter generateSummaryStream(MultipartFile file, Long userId);

    /**
     * Generate an outline in streaming mode
     *
     * @param file   document file
     * @param userId user ID
     * @return SSE event stream
     */
    SseEmitter generateOutlineStream(MultipartFile file, Long userId);

    /**
     * Generate a summary from provided content (non-streaming)
     *
     * @param dto    processing request
     * @param userId user ID
     * @return the processing result
     */
    DocumentProcessVO generateSummaryByContent(DocumentProcessDTO dto, Long userId);

    /**
     * Generate a summary from provided content in streaming mode
     *
     * @param dto    processing request
     * @param userId user ID
     * @return SSE event stream
     */
    SseEmitter generateSummaryByContentStream(DocumentProcessDTO dto, Long userId);
}