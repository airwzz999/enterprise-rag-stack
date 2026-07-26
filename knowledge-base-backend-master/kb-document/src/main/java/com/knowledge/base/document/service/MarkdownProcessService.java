package com.knowledge.base.document.service;

import java.util.List;
import java.util.Map;

/**
 * Markdown processing service interface
 *
 * <p>Used to process Markdown content, including image URL replacement, etc.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface MarkdownProcessService {

    /**
     * Processes images within Markdown content
     * <p>Automatically uploads external images to the rustfs server and replaces the URLs</p>
     *
     * @param content Markdown content
     * @return processed content and image URL mapping (original URL -> new URL)
     */
    MarkdownProcessResult processImages(String content);

    /**
     * Extracts all image URLs from Markdown
     *
     * @param content Markdown content
     * @return image URL list
     */
    List<String> extractImageUrls(String content);

    /**
     * Replaces image URLs within Markdown
     *
     * @param content        Markdown content
     * @param urlMappings    URL mapping (original URL -> new URL)
     * @return the Markdown content after replacement
     */
    String replaceImageUrls(String content, Map<String, String> urlMappings);

    /**
     * Generates a content summary (HTML format)
     *
     * @param content     Markdown content
     * @param maxLength   maximum summary length
     * @return HTML-format summary
     */
    String generateSummary(String content, int maxLength);

    /**
     * Markdown processing result
     */
    class MarkdownProcessResult {
        /**
         * Processed content
         */
        private String processedContent;

        /**
         * Image URL mapping (original URL -> new URL)
         */
        private Map<String, String> urlMappings;

        /**
         * Number of images uploaded successfully
         */
        private Integer successCount;

        /**
         * Number of images that failed to upload
         */
        private Integer failureCount;

        public MarkdownProcessResult(String processedContent, Map<String, String> urlMappings,
                                     Integer successCount, Integer failureCount) {
            this.processedContent = processedContent;
            this.urlMappings = urlMappings;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }

        public String getProcessedContent() {
            return processedContent;
        }

        public void setProcessedContent(String processedContent) {
            this.processedContent = processedContent;
        }

        public Map<String, String> getUrlMappings() {
            return urlMappings;
        }

        public void setUrlMappings(Map<String, String> urlMappings) {
            this.urlMappings = urlMappings;
        }

        public Integer getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(Integer successCount) {
            this.successCount = successCount;
        }

        public Integer getFailureCount() {
            return failureCount;
        }

        public void setFailureCount(Integer failureCount) {
            this.failureCount = failureCount;
        }
    }
}
