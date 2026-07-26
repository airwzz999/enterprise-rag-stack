package com.knowledge.base.document.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * File parsing Service
 *
 * <p>Parses uploaded PDF / Word / Excel / PPT / plain text files into Markdown text.
 * The parsed content can be stored directly into the knowledge base and go through the
 * subsequent RAG / KAG pipeline.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface FileParserService {

    /**
     * Parses the file content and returns Markdown-formatted text
     *
     * @param file uploaded file
     * @return parsed text content
     * @throws Exception thrown on parsing errors
     */
    String parse(MultipartFile file) throws Exception;

    /**
     * Determines whether a file extension is supported for parsing
     *
     * @param extension file extension (without the dot, e.g. "pdf", "docx")
     * @return true = parsing supported
     */
    boolean isSupported(String extension);
}
