package com.knowledge.base.document.service.impl;

import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.document.entity.mongodb.DocumentContent;
import com.knowledge.base.document.repository.mongodb.DocumentContentRepository;
import com.knowledge.base.document.service.DocumentContentService;
import com.knowledge.base.document.service.MarkdownProcessService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Document content service implementation class
 *
 * <p>Manages document content stored in MongoDB</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class DocumentContentServiceImpl implements DocumentContentService {

    @Resource
    private DocumentContentRepository documentContentRepository;

    @Resource
    private MarkdownProcessService markdownProcessService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveContent(Long documentId, String content) {
        log.info("Save document content: documentId={}, contentLength={}", documentId, content != null ? content.length() : 0);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("Document content must not be empty");
        }

        // Process content (upload external images, etc.)
        String processedContent = processContent(content);

        // Build the document content entity
        DocumentContent documentContent = DocumentContent.builder()
                .documentId(documentId)
                .content(processedContent)
                .contentLength(processedContent.length())
                .contentSummary(generateContentSummary(processedContent))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(1)
                .deleted(false)
                .build();

        // Save to MongoDB
        DocumentContent saved = documentContentRepository.save(documentContent);

        log.info("Document content saved successfully: documentId={}, contentId={}", documentId, saved.getId());

        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateContent(Long documentId, String content) {
        log.info("Update document content: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("Document content must not be empty");
        }

        // Find existing content
        DocumentContent existContent = documentContentRepository.findByDocumentId(documentId);
        if (existContent == null) {
            // Create new content if it does not exist
            saveContent(documentId, content);
            return true;
        }

        // Process content (upload external images, etc.)
        String processedContent = processContent(content);

        // Update the content
        existContent.setContent(processedContent);
        existContent.setContentLength(processedContent.length());
        existContent.setContentSummary(generateContentSummary(processedContent));
        existContent.setUpdatedAt(LocalDateTime.now());
        existContent.setVersion(existContent.getVersion() + 1);

        documentContentRepository.save(existContent);

        log.info("Document content updated successfully: documentId={}, contentId={}", documentId, existContent.getId());

        return true;
    }

    @Override
    public DocumentContent getContentByDocumentId(Long documentId) {
        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        DocumentContent content = documentContentRepository.findByDocumentId(documentId);
        if (content == null) {
            throw new BusinessException("Document content does not exist");
        }

        return content;
    }

    @Override
    public DocumentContent getContentById(String contentId) {
        if (contentId == null) {
            throw new BusinessException("Content ID must not be null");
        }

        return documentContentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException("Document content does not exist"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteContent(Long documentId) {
        log.info("Delete document content: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Soft delete
        DocumentContent content = documentContentRepository.findByDocumentId(documentId);
        if (content != null) {
            content.setDeleted(true);
            content.setUpdatedAt(LocalDateTime.now());
            documentContentRepository.save(content);
        }

        return true;
    }

    @Override
    public String processContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        // Normalize Markdown content: remove common leading indentation from pasted/imported content,
        // preventing Markdown syntax such as headings from being misparsed as indented code blocks.
        String normalized = normalizeMarkdown(content);

        // Process images within the Markdown (upload external images)
        MarkdownProcessService.MarkdownProcessResult result = markdownProcessService.processImages(normalized);

        if (result.getFailureCount() > 0) {
            log.warn("Some images failed to upload: {} succeeded, {} failed",
                    result.getSuccessCount(), result.getFailureCount());
        }

        return result.getProcessedContent();
    }

    /**
     * Normalizes Markdown content, removing common leading indentation from pasted/imported content.
     *
     * <p>When a user copies content from a web page, IDE, or document and pastes it into the
     * editor, it may carry extra indentation. The CommonMark spec treats lines starting with
     * 4 or more spaces as indented code blocks, causing Markdown syntax such as headings (#) to
     * be rendered as code instead of headings.</p>
     *
     * <p>This method is analogous to Python's textwrap.dedent: it finds the minimum common
     * indentation across all non-blank lines and removes it from the start of each line.
     * If any non-blank line has no leading whitespace, no changes are made (indicating the
     * user intended it that way).</p>
     *
     * @param text original Markdown text
     * @return Markdown text with common indentation removed
     */
    private String normalizeMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String[] lines = text.split("\n", -1);

        // Find the minimum indentation across all non-blank lines
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            int indent = 0;
            while (indent < line.length() && (line.charAt(indent) == ' ' || line.charAt(indent) == '\t')) {
                indent++;
            }
            minIndent = Math.min(minIndent, indent);
        }

        // No indentation or no content, no processing needed
        if (minIndent == Integer.MAX_VALUE || minIndent == 0) {
            return text;
        }

        // Remove the common indentation from each line
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                result.append(line);
            } else {
                int stripLen = Math.min(minIndent, line.length());
                result.append(line.substring(stripLen));
            }
            if (i < lines.length - 1) {
                result.append('\n');
            }
        }

        return result.toString();
    }

    /**
     * Generates a content summary
     *
     * @param content Markdown content
     * @return summary
     */
    private String generateContentSummary(String content) {
        return markdownProcessService.generateSummary(content, 500);
    }
}
