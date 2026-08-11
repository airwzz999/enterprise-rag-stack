package com.knowledge.base.ai.rag.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.knowledge.base.ai.config.RagProperties;
import com.knowledge.base.ai.rag.entity.DocumentChunk;
import com.knowledge.base.ai.rag.service.ChunkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Document chunking implementation
 *
 * <p>Uses a hybrid paragraph-aware + fixed-size chunking strategy:
 * <ol>
 *   <li>Split into sections by Markdown headings (# ## ###)</li>
 *   <li>Within each section, split into paragraphs by blank lines (\n\n)</li>
 *   <li>Merge/split paragraphs by token count to reach the target chunkSize</li>
 *   <li>Each chunk includes the nearest heading as context</li>
 * </ol></p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkingServiceImpl implements ChunkingService {

    private final RagProperties ragProperties;

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    /** Roughly 1 token = 0.75 Chinese characters; use a conservative estimate of 1 token ≈ 2 characters (mixed Chinese/English) */
    private static final double CHARS_PER_TOKEN = 2.0;

    /** {@inheritDoc} */
    @Override
    public List<DocumentChunk> chunk(String content, Long documentId, String documentTitle,
                                      Long categoryId, Long authorId, Long teamId, Integer docStatus,
                                      Integer isPublic, String publishTime) {
        if (content == null || content.isEmpty()) {
            log.warn("Document content is empty, skipping chunking: documentId={}", documentId);
            return List.of();
        }

        int chunkSize = ragProperties.getChunking().getChunkSize();
        int chunkOverlap = ragProperties.getChunking().getChunkOverlap();
        int maxChars = (int) (chunkSize * CHARS_PER_TOKEN);
        int overlapChars = (int) (chunkOverlap * CHARS_PER_TOKEN);

        List<Section> sections = splitByHeadings(content);
        List<DocumentChunk> chunks = new ArrayList<>();

        for (Section section : sections) {
            List<String> paragraphs = splitParagraphs(section.content);
            List<String> merged = mergeParagraphs(paragraphs, maxChars);
            for (int i = 0; i < merged.size(); i++) {
                String chunkContent = (section.heading != null ? section.heading + "\n\n" : "") + merged.get(i);

                // Add overlap: take overlapChars characters from the end of the previous chunk
                if (i > 0 && overlapChars > 0) {
                    String prevContent = merged.get(i - 1);
                    if (prevContent.length() > overlapChars) {
                        chunkContent = prevContent.substring(prevContent.length() - overlapChars) + "\n" + chunkContent;
                    }
                }

                chunks.add(DocumentChunk.builder()
                        .chunkId(UUID.randomUUID().toString())
                        .documentId(documentId)
                        .documentTitle(documentTitle)
                        .content(chunkContent)
                        .heading(section.heading)
                        .chunkIndex(chunks.size())
                        .categoryId(categoryId)
                        .authorId(authorId)
                        .teamId(teamId)
                        .docStatus(docStatus)
                        .isPublic(isPublic)
                        .publishTime(publishTime)
                        .build());
            }
        }

        // Set totalChunks
        final int total = chunks.size();
        chunks.forEach(c -> c.setTotalChunks(total));

        log.debug("Document chunking completed: documentId={}, totalChunks={}", documentId, total);
        return chunks;
    }

    /**
     * Split into sections by Markdown headings
     */
    private List<Section> splitByHeadings(String content) {
        List<Section> sections = new ArrayList<>();
        Matcher m = HEADING_PATTERN.matcher(content);

        int lastEnd = 0;
        String currentHeading = null;

        while (m.find()) {
            if (lastEnd < m.start()) {
                String sectionContent = content.substring(lastEnd, m.start()).trim();
                if (!sectionContent.isEmpty()) {
                    sections.add(new Section(currentHeading, sectionContent));
                }
            }
            currentHeading = m.group(2);
            lastEnd = m.end();
        }

        // Last section
        if (lastEnd < content.length()) {
            String sectionContent = content.substring(lastEnd).trim();
            if (!sectionContent.isEmpty()) {
                sections.add(new Section(currentHeading, sectionContent));
            }
        }

        // If no heading was found, treat the entire content as a single untitled section
        if (sections.isEmpty()) {
            sections.add(new Section(null, content.trim()));
        }

        return sections;
    }

    /**
     * Split into paragraphs by blank lines
     */
    private List<String> splitParagraphs(String content) {
        List<String> paragraphs = new ArrayList<>();
        for (String para : content.split("\n\n")) {
            String trimmed = para.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }
        return paragraphs;
    }

    /**
     * Merge paragraphs to reach the target size
     */
    private List<String> mergeParagraphs(List<String> paragraphs, int maxChars) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            // If the paragraph itself exceeds maxChars, split it by sentence
            if (paragraph.length() > maxChars) {
                if (current.length() > 0) {
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                }
                List<String> subParts = splitLongParagraph(paragraph, maxChars);
                result.addAll(subParts);
                continue;
            }

            if (current.length() + paragraph.length() + 2 <= maxChars) {
                if (current.length() > 0) {
                    current.append("\n\n");
                }
                current.append(paragraph);
            } else {
                if (current.length() > 0) {
                    result.add(current.toString().trim());
                }
                current = new StringBuilder(paragraph);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    /**
     * Split an overly long paragraph by sentence
     */
    private List<String> splitLongParagraph(String paragraph, int maxChars) {
        List<String> result = new ArrayList<>();
        // Split by period, question mark, exclamation mark, or newline
        String[] sentences = paragraph.split("(?<=[。！？\\.!?\\n])");
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (current.length() + trimmed.length() > maxChars) {
                if (current.length() > 0) {
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                }
                // If a single sentence is itself too long, force-truncate it
                if (trimmed.length() > maxChars) {
                    for (int i = 0; i < trimmed.length(); i += maxChars) {
                        int end = Math.min(i + maxChars, trimmed.length());
                        result.add(trimmed.substring(i, end));
                    }
                } else {
                    current = new StringBuilder(trimmed);
                }
            } else {
                if (current.length() > 0) {
                    current.append(" ");
                }
                current.append(trimmed);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    /**
     * Section inner class
     */
    private record Section(String heading, String content) {}
}
