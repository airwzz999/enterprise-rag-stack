package com.knowledge.base.ai.rag.kag.extraction;

import com.knowledge.base.ai.dto.kag.extraction.ExtractionResult;

import java.util.List;

/**
 * Entity and relation extraction service interface
 *
 * <p>Extracts knowledge entities and relations from document chunks, providing
 * the foundational data for building the knowledge graph.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface ExtractionService {

    /**
     * Extract entities and relations from a single document chunk
     *
     * @param content       the chunk text content
     * @param heading       the section heading it belongs to
     * @param docId         the source document ID
     * @param documentTitle the document title
     * @return the extraction result
     */
    ExtractionResult extract(String content, String heading, Long docId, String documentTitle);

    /**
     * Batch extraction (processes multiple chunks in a single LLM call, reducing API calls)
     *
     * @param contents list of chunk contents, each element being {content, heading, docId, documentTitle}
     * @return list of extraction results
     */
    List<ExtractionResult> extractBatch(List<ExtractionInput> contents);

    /**
     * Extraction input wrapper
     */
    record ExtractionInput(String content, String heading, Long docId, String documentTitle) {}
}
