package com.knowledge.base.ai.service.impl;

import com.knowledge.base.ai.config.ModelProvider;
import com.knowledge.base.ai.dto.DocumentProcessDTO;
import com.knowledge.base.ai.service.AiDocumentService;
import com.knowledge.base.ai.vo.DocumentProcessVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * AI document processing service implementation
 *
 * <p>Designed following the Alibaba Java Development Guidelines, implementing
 * AI document processing business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiDocumentServiceImpl implements AiDocumentService {

    private final ModelProvider modelProvider;
    private final StringRedisTemplate redisTemplate;

    private static final String SUMMARY_CACHE_PREFIX = "ai:summary:";
    private static final Duration SUMMARY_CACHE_TTL = Duration.ofDays(7);

    /**
     * Compute the cache key for a content summary, deduplicated by content MD5
     *
     * @param content document content
     * @return the cache key
     */
    private String buildSummaryCacheKey(String content) {
        return SUMMARY_CACHE_PREFIX + DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get the summary from the Redis cache; returns the summary content on a hit, null on a miss
     *
     * @param content document content
     * @return the cached summary content, or null on a cache miss
     */
    private String getCachedSummary(String content) {
        try {
            if (redisTemplate == null) return null;
            return redisTemplate.opsForValue().get(buildSummaryCacheKey(content));
        } catch (Exception e) {
            log.warn("Failed to read summary cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Write the summary to the Redis cache
     *
     * @param content document content
     * @param summary summary content
     */
    private void cacheSummary(String content, String summary) {
        try {
            if (redisTemplate == null) return;
            redisTemplate.opsForValue().set(buildSummaryCacheKey(content), summary, SUMMARY_CACHE_TTL);
            log.info("Summary cached: key={}", buildSummaryCacheKey(content));
        } catch (Exception e) {
            log.warn("Failed to write summary cache: {}", e.getMessage());
        }
    }

    /**
     * Generate a document summary
     *
     * @param content document content
     * @param length  summary length
     * @return the summary content
     */
    @Override
    public String generateSummary(String content, Integer length) {
        log.info("Generating document summary: contentLength={}, summaryLength={}", content.length(), length);

        if (length == null || length <= 0) {
            length = 200;
        }

        // Truncate long content, consistent with generateSummaryByContent
        String truncContent = truncateContent(content, 8000);

        // Check the Redis cache first
        String cached = getCachedSummary(truncContent);
        if (cached != null) {
            log.info("Summary cache hit");
            return cached;
        }

        String prompt = String.format("""
                Please generate a concise summary of the following document, with these requirements:
                1. The summary should be about %d characters long
                2. Cover the document's core content and main points
                3. Use clear, concise language
                4. Do not add any extra explanation

                Document content:
                %s
                """, length, truncContent);

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            Response<AiMessage> response = modelProvider.getDefaultModel().generate(userMessage);
            String summary = response.content().text().trim();

            // Write to the Redis cache
            cacheSummary(truncContent, summary);

            return summary;
        } catch (Exception e) {
            log.error("Failed to generate summary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate summary: " + e.getMessage());
        }
    }

    /**
     * Generate a document outline
     *
     * @param content document content
     * @param level   outline level
     * @return the outline content
     */
    @Override
    public String generateOutline(String content, Integer level) {
        log.info("Generating document outline: contentLength={}, level={}", content.length(), level);

        if (level == null || level <= 0 || level > 3) {
            level = 2;
        }

        String prompt = String.format("""
                Please generate a structured outline for the following document, with these requirements:
                1. The outline should have %d levels
                2. Use standard heading level formatting (e.g. #, ##, ###)
                3. Reflect the document's logical structure and main content
                4. Mark each top level with "I.", "II.", etc.
                5. Do not add any extra explanation

                Document content:
                %s
                """, level, content);

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            Response<AiMessage> response = modelProvider.getDefaultModel().generate(userMessage);
            return response.content().text().trim();
        } catch (Exception e) {
            log.error("Failed to generate outline: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate outline: " + e.getMessage());
        }
    }

    /**
     * Expand content
     *
     * @param content document content
     * @param expType expansion type
     * @return the expanded content
     */
    @Override
    public String expandContent(String content, String expType) {
        log.info("Expanding content: contentLength={}, expType={}", content.length(), expType);

        String typeDesc = getTypeDescription(expType);
        String prompt = String.format("""
                Please %s the following document content, with these requirements:
                1. Keep the original document's style and tone
                2. The expanded content should fit the document's topic
                3. The expanded content should be coherent and logically clear
                4. Do not add any extra explanation

                Original document content:
                %s
                """, typeDesc, content);

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            Response<AiMessage> response = modelProvider.getDefaultModel().generate(userMessage);
            return response.content().text().trim();
        } catch (Exception e) {
            log.error("Failed to expand content: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to expand content: " + e.getMessage());
        }
    }

    /**
     * Improve wording
     *
     * @param content document content
     * @param target  optimization target
     * @return the improved content
     */
    @Override
    public String optimizeContent(String content, String target) {
        log.info("Optimizing content: contentLength={}, target={}", content.length(), target);

        String targetDesc = getOptimizationDescription(target);
        String prompt = String.format("""
                Please optimize the following document content, with these requirements:
                %s
                5. Preserve the original document's core meaning and information
                6. The optimized content should be more professional and readable
                7. Do not add any extra explanation

                Original document content:
                %s
                """, targetDesc, content);

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            Response<AiMessage> response = modelProvider.getDefaultModel().generate(userMessage);
            return response.content().text().trim();
        } catch (Exception e) {
            log.error("Failed to optimize content: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to optimize content: " + e.getMessage());
        }
    }

    /**
     * Add examples
     *
     * @param content document content
     * @param expType example type
     * @return the content with examples added
     */
    @Override
    public String addExample(String content, String expType) {
        log.info("Adding examples: contentLength={}, expType={}", content.length(), expType);

        String typeDesc = getExampleDescription(expType);
        String prompt = String.format("""
                Please add appropriate examples to the following document content, with these requirements:
                %s
                3. Examples should be closely related to the document content
                4. Examples should be specific and practical
                5. Insert examples at appropriate locations
                6. Return the complete document content (including the new examples)
                7. Do not add any extra explanation

                Original document content:
                %s
                """, typeDesc, content);

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            Response<AiMessage> response = modelProvider.getDefaultModel().generate(userMessage);
            return response.content().text().trim();
        } catch (Exception e) {
            log.error("Failed to add examples: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add examples: " + e.getMessage());
        }
    }

    /**
     * Process a document (unified entry point)
     *
     * @param processDTO processing request
     * @return the processing result
     */
    @Override
    public DocumentProcessVO processDocument(DocumentProcessDTO processDTO) {
        log.info("Processing document: processType={}", processDTO.getProcessType());

        DocumentProcessVO result = new DocumentProcessVO();
        result.setProcessType(processDTO.getProcessType());

        try {
            String processedContent;

            switch (processDTO.getProcessType()) {
                case "summary":
                    Integer summaryLength = processDTO.getProcessParams() != null
                            ? processDTO.getProcessParams().getSummaryLength()
                            : 200;
                    processedContent = generateSummary(processDTO.getContent(), summaryLength);
                    result.setProcessedContent(processedContent);
                    break;

                case "outline":
                    Integer outlineLevel = processDTO.getProcessParams() != null
                            ? processDTO.getProcessParams().getOutlineLevel()
                            : 2;
                    processedContent = generateOutline(processDTO.getContent(), outlineLevel);
                    result.setProcessedContent(processedContent);
                    break;

                case "expansion":
                    String expansionType = processDTO.getProcessParams() != null
                            ? processDTO.getProcessParams().getExpansionType()
                            : "detail";
                    processedContent = expandContent(processDTO.getContent(), expansionType);
                    result.setProcessedContent(processedContent);
                    break;

                case "optimization":
                    String optimizationTarget = processDTO.getProcessParams() != null
                            ? processDTO.getProcessParams().getOptimizationTarget()
                            : "readability";
                    processedContent = optimizeContent(processDTO.getContent(), optimizationTarget);
                    result.setProcessedContent(processedContent);
                    break;

                case "example":
                    String exampleType = processDTO.getProcessParams() != null
                            ? processDTO.getProcessParams().getExampleType()
                            : "code";
                    processedContent = addExample(processDTO.getContent(), exampleType);
                    result.setProcessedContent(processedContent);
                    break;

                default:
                    throw new RuntimeException("Unsupported processing type: " + processDTO.getProcessType());
            }

            result.setSuccess(true);
            result.setMessage("Processing succeeded");

        } catch (Exception e) {
            log.error("Failed to process document: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Processing failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Generate a document summary (file upload variant)
     *
     * @param file   document file
     * @param userId user ID
     * @return the processing result
     */
    @Override
    public DocumentProcessVO generateSummary(MultipartFile file, Long userId) {
        log.info("Generating document summary (file upload): fileName={}, userId={}", file.getOriginalFilename(), userId);

        DocumentProcessVO result = new DocumentProcessVO();
        result.setProcessType("summary");

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String summary = generateSummary(content, 200);

            result.setProcessedContent(summary);
            result.setSuccess(true);
            result.setMessage("Generated successfully");

        } catch (IOException e) {
            log.error("Failed to read file: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Failed to read file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate summary: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Generation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Generate a document outline (file upload variant)
     *
     * @param file   document file
     * @param userId user ID
     * @return the processing result
     */
    @Override
    public DocumentProcessVO generateOutline(MultipartFile file, Long userId) {
        log.info("Generating document outline (file upload): fileName={}, userId={}", file.getOriginalFilename(), userId);

        DocumentProcessVO result = new DocumentProcessVO();
        result.setProcessType("outline");

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String outline = generateOutline(content, 2);

            result.setProcessedContent(outline);
            result.setSuccess(true);
            result.setMessage("Generated successfully");

        } catch (IOException e) {
            log.error("Failed to read file: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Failed to read file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate outline: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Generation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Expand content (DTO variant)
     *
     * @param dto    processing request
     * @param userId user ID
     * @return the processing result
     */
    @Override
    public DocumentProcessVO expandContent(DocumentProcessDTO dto, Long userId) {
        log.info("Expanding content (DTO variant): userId={}", userId);

        DocumentProcessVO result = new DocumentProcessVO();
        result.setProcessType("expansion");

        try {
            String expType = dto.getProcessParams() != null ? dto.getProcessParams().getExpansionType() : "detail";
            String expanded = expandContent(dto.getContent(), expType);

            result.setProcessedContent(expanded);
            result.setSuccess(true);
            result.setMessage("Expansion succeeded");

        } catch (Exception e) {
            log.error("Failed to expand content: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Expansion failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Improve wording (DTO variant)
     *
     * @param dto    processing request
     * @param userId user ID
     * @return the processing result
     */
    @Override
    public DocumentProcessVO optimizeContent(DocumentProcessDTO dto, Long userId) {
        log.info("Optimizing content (DTO variant): userId={}", userId);

        DocumentProcessVO result = new DocumentProcessVO();
        result.setProcessType("optimization");

        try {
            String target = dto.getProcessParams() != null ? dto.getProcessParams().getOptimizationTarget() : "readability";
            String optimized = optimizeContent(dto.getContent(), target);

            result.setProcessedContent(optimized);
            result.setSuccess(true);
            result.setMessage("Optimization succeeded");

        } catch (Exception e) {
            log.error("Failed to optimize content: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Optimization failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Generate a summary in streaming mode
     *
     * @param file   document file
     * @param userId user ID
     * @return SSE event stream
     */
    @Override
    public SseEmitter generateSummaryStream(MultipartFile file, Long userId) {
        log.info("Generating summary in streaming mode: fileName={}, userId={}", file.getOriginalFilename(), userId);

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String summary = generateSummary(content, 200);

            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(summary));
            emitter.complete();

        } catch (Exception e) {
            log.error("Failed to generate summary in streaming mode: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Generate an outline in streaming mode
     *
     * @param file   document file
     * @param userId user ID
     * @return SSE event stream
     */
    @Override
    public SseEmitter generateOutlineStream(MultipartFile file, Long userId) {
        log.info("Generating outline in streaming mode: fileName={}, userId={}", file.getOriginalFilename(), userId);

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String outline = generateOutline(content, 2);

            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(outline));
            emitter.complete();

        } catch (Exception e) {
            log.error("Failed to generate outline in streaming mode: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Content truncation: for overly long documents, take the head and tail and join them, reducing LLM token consumption
     *
     * @param content  the original content
     * @param maxChars maximum character count
     * @return the truncated content
     */
    private String truncateContent(String content, int maxChars) {
        if (content == null || content.length() <= maxChars) {
            return content;
        }
        int headLen = (int) (maxChars * 0.75);
        int tailLen = maxChars - headLen;
        String head = content.substring(0, headLen);
        String tail = content.substring(content.length() - tailLen);
        return head + "\n\n...\n\n" + tail;
    }

    /**
     * Split text into sentence chunks by period/newline, for streaming sentence-by-sentence output
     *
     * @param text the original text
     * @return list of sentence chunks
     */
    static List<String> chunkBySentence(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);
            if (c == '。' || c == '\n' || c == '！' || c == '？') {
                String chunk = current.toString().trim();
                if (!chunk.isEmpty()) {
                    chunks.add(chunk);
                }
                current.setLength(0);
            }
        }
        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            chunks.add(remaining);
        }
        return chunks;
    }

    /**
     * Generate a summary from provided content (non-streaming)
     *
     * @param dto    processing request
     * @param userId user ID
     * @return the processing result
     */
    @Override
    public DocumentProcessVO generateSummaryByContent(DocumentProcessDTO dto, Long userId) {
        log.info("Generating summary from content (non-streaming): userId={}, title={}", userId, dto.getTitle());

        DocumentProcessVO result = new DocumentProcessVO();
        result.setProcessType("summary");

        try {
            String content = truncateContent(dto.getContent(), 8000);

            // Check the Redis cache first
            String cached = getCachedSummary(content);
            if (cached != null) {
                log.info("Summary cache hit: contentMd5={}", DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8)));
                result.setProcessedContent(cached);
                result.setOriginalContent(dto.getContent());
                result.setSuccess(true);
                result.setMessage("Cache hit");
                return result;
            }

            Integer length = dto.getProcessParams() != null
                    ? dto.getProcessParams().getSummaryLength()
                    : 200;
            if (length == null || length <= 0) {
                length = 200;
            }

            String prompt = buildSummaryPrompt(content, dto.getTitle(), length);
            UserMessage userMessage = UserMessage.from(prompt);
            Response<AiMessage> response = modelProvider.getDefaultModel().generate(userMessage);
            String summary = response.content().text().trim();

            // Write to the Redis cache
            cacheSummary(content, summary);

            result.setProcessedContent(summary);
            result.setOriginalContent(dto.getContent());
            result.setSuccess(true);
            result.setMessage("Generated successfully");

        } catch (Exception e) {
            log.error("Failed to generate summary from content: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Generation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Generate a summary from provided content in streaming mode
     *
     * @param dto    processing request
     * @param userId user ID
     * @return SSE event stream
     */
    @Override
    public SseEmitter generateSummaryByContentStream(DocumentProcessDTO dto, Long userId) {
        log.info("Generating summary from content in streaming mode: userId={}, title={}", userId, dto.getTitle());

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        try {
            String content = truncateContent(dto.getContent(), 8000);

            // Check the Redis cache first; on a hit, stream the cached summary directly
            String cached = getCachedSummary(content);
            if (cached != null) {
                log.info("Summary cache hit (streaming): contentMd5={}", DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8)));
                new Thread(() -> {
                    try {
                        List<String> chunks = chunkBySentence(cached);
                        for (int i = 0; i < chunks.size(); i++) {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(chunks.get(i)));
                            if (i < chunks.size() - 1) {
                                Thread.sleep(30);
                            }
                        }
                        DocumentProcessVO result = new DocumentProcessVO();
                        result.setProcessType("summary");
                        result.setProcessedContent(cached);
                        result.setSuccess(true);
                        result.setMessage("Cache hit");
                        emitter.send(SseEmitter.event().name("done").data(result));
                        emitter.complete();
                    } catch (Exception e) {
                        try {
                            emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                        } catch (IOException ignored) {}
                        emitter.completeWithError(e);
                    }
                }, "summary-stream-cached-" + userId).start();
                return emitter;
            }

            Integer lengthParam = dto.getProcessParams() != null
                    ? dto.getProcessParams().getSummaryLength()
                    : 200;
            final Integer length = (lengthParam == null || lengthParam <= 0) ? 200 : lengthParam;

            // Run asynchronously in a separate thread to avoid blocking the Controller thread
            final String finalContent = content;
            new Thread(() -> {
                try {
                    String prompt = buildSummaryPrompt(finalContent, dto.getTitle(), length);
                    UserMessage userMessage = UserMessage.from(prompt);
                    Response<AiMessage> response = modelProvider.getDefaultModel().generate(userMessage);
                    String summary = response.content().text().trim();

                    // Write to the Redis cache
                    cacheSummary(finalContent, summary);

                    // Split into sentences and stream them one by one
                    List<String> chunks = chunkBySentence(summary);
                    for (int i = 0; i < chunks.size(); i++) {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(chunks.get(i)));
                        // Small gap between sentences to simulate a typing effect
                        if (i < chunks.size() - 1) {
                            Thread.sleep(40);
                        }
                    }

                    // Send the completion event
                    DocumentProcessVO result = new DocumentProcessVO();
                    result.setProcessType("summary");
                    result.setProcessedContent(summary);
                    result.setSuccess(true);
                    result.setMessage("Generated successfully");
                    emitter.send(SseEmitter.event()
                            .name("done")
                            .data(result));
                    emitter.complete();

                } catch (Exception e) {
                    log.error("Failed to generate summary in streaming mode: {}", e.getMessage(), e);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("Generation failed: " + e.getMessage()));
                        emitter.completeWithError(e);
                    } catch (IOException ex) {
                        emitter.completeWithError(ex);
                    }
                }
            }, "summary-stream-" + userId).start();

        } catch (Exception e) {
            log.error("Failed to start streaming summary generation: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Build the summary prompt
     *
     * @param content document content
     * @param title   document title
     * @param length  summary length
     * @return the prompt text
     */
    private String buildSummaryPrompt(String content, String title, int length) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please generate a concise summary of the following document, with these requirements:\n");
        sb.append("1. The summary should be about ").append(length).append(" characters long\n");
        sb.append("2. Cover the document's core content and main points\n");
        sb.append("3. Use clear, concise language\n");
        sb.append("4. Do not add any extra explanation\n");
        if (title != null && !title.isEmpty()) {
            sb.append("\nDocument title: ").append(title).append("\n");
        }
        sb.append("\nDocument content:\n").append(content);
        return sb.toString();
    }

    private String getTypeDescription(String expType) {
        return switch (expType != null ? expType.toLowerCase() : "") {
            case "detail" -> "elaborate on each point in detail, adding more detail and explanation";
            case "example" -> "add concrete examples for key concepts";
            case "theory" -> "add relevant theoretical basis and references";
            case "practice" -> "supplement with practical cases and application scenarios";
            default -> "expand the content to make it more complete and rich";
        };
    }

    private String getOptimizationDescription(String target) {
        return switch (target != null ? target.toLowerCase() : "") {
            case "readability" -> "1. Improve the document's readability and clarity\n2. Use clearer, more concise language\n3. Optimize sentence structure\n4. Add paragraph breaks where appropriate";
            case "professional" -> "1. Use more professional terminology and phrasing\n2. Optimize the document's format and structure\n3. Improve the document's professionalism and authority\n4. Use a formal tone";
            case "seo" -> "1. Optimize keyword usage\n2. Improve content relevance\n3. Add appropriate headings and subheadings\n4. Optimize content structure";
            default -> "1. Improve the document's overall quality\n2. Optimize wording\n3. Improve document structure\n4. Enhance the effectiveness of the information conveyed";
        };
    }

    private String getExampleDescription(String expType) {
        return switch (expType != null ? expType.toLowerCase() : "") {
            case "code" -> "1. Add code examples\n2. Code should have explanatory comments\n3. Code formatting should be standard";
            case "case" -> "1. Add real-world cases\n2. Cases should be representative\n3. Cases should have detailed explanations";
            case "data" -> "1. Add data examples\n2. Data should be authentic and credible\n3. Data should have a stated source";
            default -> "1. Add appropriate examples\n2. Examples should be relevant to the content\n3. Examples should include an explanation";
        };
    }
}