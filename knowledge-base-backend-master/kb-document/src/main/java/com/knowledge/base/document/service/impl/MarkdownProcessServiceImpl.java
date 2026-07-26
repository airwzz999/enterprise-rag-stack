package com.knowledge.base.document.service.impl;

import com.knowledge.base.document.service.FileUploadService;
import com.knowledge.base.document.service.MarkdownProcessService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown processing service implementation class
 *
 * <p>Processes Markdown content, including image URL replacement</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class MarkdownProcessServiceImpl implements MarkdownProcessService {

    @Resource
    private FileUploadService fileUploadService;

    /**
     * Markdown image regex
     * Matches the format: ![alt](url) or ![alt](url "title")
     */
    private static final Pattern IMAGE_PATTERN = Pattern.compile(
            "!\\[([^\\]]*)\\]\\(([^)]+)\\)"
    );

    /**
     * HTML image tag regex
     * Matches the format: <img src="url" />
     */
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile(
            "<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>"
    );

    @Override
    public MarkdownProcessResult processImages(String content) {
        if (!StringUtils.hasText(content)) {
            return new MarkdownProcessResult(content, Collections.emptyMap(), 0, 0);
        }

        log.info("Starting to process images within Markdown");

        // Extract all image URLs
        List<String> imageUrls = extractImageUrls(content);
        if (imageUrls.isEmpty()) {
            log.info("No images found to process");
            return new MarkdownProcessResult(content, Collections.emptyMap(), 0, 0);
        }

        log.info("Found {} image URLs", imageUrls.size());

        // Upload external images and build the URL mapping
        Map<String, String> urlMappings = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        for (String imageUrl : imageUrls) {
            if (fileUploadService.isExternalImageUrl(imageUrl)) {
                try {
                    String newUrl = fileUploadService.uploadImageFromUrl(imageUrl);
                    urlMappings.put(imageUrl, newUrl);
                    successCount++;
                    log.info("Image uploaded successfully: {} -> {}", imageUrl, newUrl);
                } catch (Exception e) {
                    log.error("Image upload failed: {}", imageUrl, e);
                    failureCount++;
                }
            }
        }

        // Replace the image URLs within the Markdown
        String processedContent = replaceImageUrls(content, urlMappings);

        log.info("Image processing complete: {} succeeded, {} failed", successCount, failureCount);

        return new MarkdownProcessResult(processedContent, urlMappings, successCount, failureCount);
    }

    @Override
    public List<String> extractImageUrls(String content) {
        if (!StringUtils.hasText(content)) {
            return Collections.emptyList();
        }

        Set<String> urls = new LinkedHashSet<>();

        // Extract Markdown-format images
        Matcher matcher = IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            String url = matcher.group(2);
            // Remove the title portion from the URL
            int spaceIndex = url.indexOf(' ');
            if (spaceIndex > 0) {
                url = url.substring(0, spaceIndex);
            }
            urls.add(url.trim());
        }

        // Extract HTML-format images
        matcher = HTML_IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            String url = matcher.group(1);
            urls.add(url);
        }

        return new ArrayList<>(urls);
    }

    @Override
    public String replaceImageUrls(String content, Map<String, String> urlMappings) {
        if (urlMappings == null || urlMappings.isEmpty()) {
            return content;
        }

        String result = content;

        // Replace Markdown-format image URLs
        for (Map.Entry<String, String> entry : urlMappings.entrySet()) {
            String oldUrl = entry.getKey();
            String newUrl = entry.getValue();

            // Escape regex special characters
            String escapedOldUrl = Pattern.quote(oldUrl);

            // Replace the ![alt](oldUrl) format
            result = result.replaceAll(
                    "(!\\[[^\\]]*\\]\\()" + escapedOldUrl + "(\\))",
                    "$1" + newUrl + "$2"
            );

            // Replace HTML-format image URLs
            result = result.replaceAll(
                    "(<img[^>]+src=[\"'])" + escapedOldUrl + "([\"'][^>]*>)",
                    "$1" + newUrl + "$2"
            );
        }

        return result;
    }

    @Override
    public String generateSummary(String content, int maxLength) {
        if (!StringUtils.hasText(content)) {
            return "";
        }

        // Simple Markdown-to-HTML conversion (removes images, code blocks, etc.)
        String summary = content
                // Remove images
                .replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "")
                // Remove code blocks
                .replaceAll("```[\\s\\S]*?```", "")
                // Remove inline code
                .replaceAll("`[^`]+`", "")
                // Remove links
                .replaceAll("\\[[^\\]]+\\]\\([^)]+\\)", "")
                // Remove heading symbols
                .replaceAll("^#+\\s*", "")
                // Remove bold, italic, and other formatting symbols
                .replaceAll("[*_*#]+", "")
                // Remove extra blank lines
                .replaceAll("\\n+", "\n")
                .trim();

        // Truncate to the specified length
        if (summary.length() > maxLength) {
            summary = summary.substring(0, maxLength) + "...";
        }

        return summary;
    }
}
