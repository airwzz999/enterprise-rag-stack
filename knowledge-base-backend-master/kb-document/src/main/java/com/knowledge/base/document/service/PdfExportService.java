package com.knowledge.base.document.service;

import java.util.List;

/**
 * PDF export service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface PdfExportService {

    /**
     * Exports a document to PDF
     *
     * @param documentId document ID
     * @return PDF download URL
     */
    String exportDocumentToPdf(Long documentId);

    /**
     * Exports a document to PDF (returns the byte array directly)
     *
     * @param documentId document ID
     * @return PDF file byte array
     */
    byte[] exportDocumentToPdfBytes(Long documentId);

    /**
     * Generates the PDF file name
     *
     * @param documentId document ID
     * @param title document title
     * @return file name
     */
    String generatePdfFileName(Long documentId, String title);

    /**
     * Batch-exports documents
     *
     * @param documentIds document ID list (String type, to avoid JavaScript precision loss)
     * @param format export format (pdf / markdown)
     * @return ZIP file byte array
     */
    byte[] batchExportDocuments(List<String> documentIds, String format);
}
