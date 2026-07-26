package com.knowledge.base.ai.service.impl;

import com.knowledge.base.ai.config.ModelProvider;
import com.knowledge.base.ai.dto.WritingRequestDTO;
import com.knowledge.base.ai.service.AiWritingService;
import com.knowledge.base.ai.vo.WritingResultVO;
import com.knowledge.base.ai.vo.WritingTemplateVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AI writing service implementation
 *
 * <p>Designed following the Alibaba Java Development Guidelines, providing the
 * business logic for AI writing (generate, expand, optimize, continue).
 * Supports both synchronous generation and SSE streaming generation.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiWritingServiceImpl implements AiWritingService {

    private final ModelProvider modelProvider;

    /** {@inheritDoc} */
    @Override
    public WritingResultVO generate(WritingRequestDTO dto, Long userId) {
        String modelName = dto.getModel() != null ? dto.getModel() : modelProvider.getDefaultModelName();
        log.info("AI writing generation request: userId={}, model={}, actionType={}", userId, modelName, dto.getActionType());

        String prompt = buildWritingPrompt(dto);
        logModelCallParams("Generate", modelName, dto, prompt);

        ChatLanguageModel model = resolveModel(dto.getModel());

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            log.info(">>> Starting LLM call [{}]: promptLength={}", modelName, prompt.length());
            Response<AiMessage> response = model.generate(userMessage);
            String content = response.content().text().trim();

            logModelCallResult("Generate", modelName, response, content);
            return WritingResultVO.builder()
                    .content(content)
                    .tokens(response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : null)
                    .wordCount(content.length())
                    .model(modelName)
                    .build();
        } catch (Exception e) {
            log.error("AI writing generation failed: model={}, error={}", modelName, e.getMessage(), e);
            throw new RuntimeException("AI writing generation failed: " + e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public SseEmitter generateStream(WritingRequestDTO dto, Long userId) {
        String modelName = dto.getModel() != null ? dto.getModel() : modelProvider.getDefaultModelName();
        log.info("AI writing streaming generation request: userId={}, model={}", userId, modelName);

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        String prompt = buildWritingPrompt(dto);
        logModelCallParams("Streaming generate", modelName, dto, prompt);

        ChatLanguageModel model = resolveModel(dto.getModel());

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            log.info(">>> Starting LLM call [{}] (streaming): promptLength={}", modelName, prompt.length());
            Response<AiMessage> response = model.generate(userMessage);
            String fullContent = response.content().text().trim();

            logModelCallResult("Streaming generate", modelName, response, fullContent);

            // Send in character chunks to simulate streaming output
            int chunkSize = 10;
            for (int i = 0; i < fullContent.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, fullContent.length());
                String chunk = fullContent.substring(i, end);
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(chunk));
            }

            // Send the completion event
            WritingResultVO result = WritingResultVO.builder()
                    .content(fullContent)
                    .tokens(response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : null)
                    .wordCount(fullContent.length())
                    .model(modelName)
                    .build();
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(result));
            emitter.complete();

            log.info("AI writing streaming generation completed: userId={}, wordCount={}", userId, fullContent.length());
        } catch (IOException e) {
            log.error("Failed to send SSE for AI writing streaming generation: model={}, error={}", modelName, e.getMessage(), e);
            emitter.completeWithError(e);
        } catch (Exception e) {
            log.error("AI writing streaming generation failed: model={}, error={}", modelName, e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("AI writing generation failed: " + e.getMessage()));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }

        return emitter;
    }

    /** {@inheritDoc} */
    @Override
    public WritingResultVO expand(WritingRequestDTO dto, Long userId) {
        String modelName = dto.getModel() != null ? dto.getModel() : modelProvider.getDefaultModelName();
        log.info("AI writing expansion request: userId={}, model={}, topic={}", userId, modelName, dto.getTopic());

        if (dto.getExistingContent() == null || dto.getExistingContent().isEmpty()) {
            throw new RuntimeException("The expand feature requires existing content (existingContent)");
        }

        String prompt = String.format("""
                You are a professional document writing assistant. Please expand the following content, with these requirements:
                1. Keep the original topic and style
                2. Enrich the content details, adding depth and breadth
                3. Supplement relevant background information, arguments, and examples
                4. Maintain logical coherence and a clear structure
                5. Do not add any extra explanation; output the complete expanded content directly

                Original topic: %s

                Original content:
                %s
                """, dto.getTopic(), dto.getExistingContent());

        logModelCallParams("Expand", modelName, dto, prompt);
        ChatLanguageModel model = resolveModel(dto.getModel());

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            log.info(">>> Starting LLM call [{}] (expand): promptLength={}", modelName, prompt.length());
            Response<AiMessage> response = model.generate(userMessage);
            String content = response.content().text().trim();

            logModelCallResult("Expand", modelName, response, content);
            return WritingResultVO.builder()
                    .content(content)
                    .tokens(response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : null)
                    .wordCount(content.length())
                    .model(modelName)
                    .build();
        } catch (Exception e) {
            log.error("AI writing expansion failed: model={}, error={}", modelName, e.getMessage(), e);
            throw new RuntimeException("AI writing expansion failed: " + e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public WritingResultVO optimize(WritingRequestDTO dto, Long userId) {
        String modelName = dto.getModel() != null ? dto.getModel() : modelProvider.getDefaultModelName();
        log.info("AI writing optimization request: userId={}, model={}, topic={}", userId, modelName, dto.getTopic());

        if (dto.getExistingContent() == null || dto.getExistingContent().isEmpty()) {
            throw new RuntimeException("The optimize feature requires existing content (existingContent)");
        }

        String styleDesc = getStyleDescription(dto.getStyle());
        String prompt = String.format("""
                You are a professional document editing assistant. Please optimize and polish the following content, with these requirements:
                1. %s
                2. Correct grammar errors and unclear phrasing
                3. Optimize sentence structure to make the language more fluent
                4. Improve paragraph organization to enhance readability
                5. Keep the original document's core meaning and information unchanged

                Original title: %s

                Original content:
                %s
                """, styleDesc, dto.getTopic(), dto.getExistingContent());

        logModelCallParams("Optimize", modelName, dto, prompt);
        ChatLanguageModel model = resolveModel(dto.getModel());

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            log.info(">>> Starting LLM call [{}] (optimize): promptLength={}", modelName, prompt.length());
            Response<AiMessage> response = model.generate(userMessage);
            String content = response.content().text().trim();

            logModelCallResult("Optimize", modelName, response, content);
            return WritingResultVO.builder()
                    .content(content)
                    .tokens(response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : null)
                    .wordCount(content.length())
                    .model(modelName)
                    .build();
        } catch (Exception e) {
            log.error("AI writing optimization failed: model={}, error={}", modelName, e.getMessage(), e);
            throw new RuntimeException("AI writing optimization failed: " + e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public WritingResultVO continueWriting(WritingRequestDTO dto, Long userId) {
        String modelName = dto.getModel() != null ? dto.getModel() : modelProvider.getDefaultModelName();
        log.info("AI writing continuation request: userId={}, model={}, topic={}", userId, modelName, dto.getTopic());

        if (dto.getExistingContent() == null || dto.getExistingContent().isEmpty()) {
            throw new RuntimeException("The continue feature requires existing content (existingContent)");
        }

        String prompt = String.format("""
                You are a professional document writing assistant. Please continue writing from the end of the following content, with these requirements:
                1. Continue the original topic, style, and logic
                2. The content should connect naturally; do not repeat existing content
                3. Further develop the discussion or supplement subsequent content
                4. Maintain a complete structure and clear logic
                5. Do not add any extra explanation; output the continuation directly

                Original topic: %s

                Existing content:
                %s

                Please continue writing from the end of the above content.
                """, dto.getTopic(), dto.getExistingContent());

        logModelCallParams("Continue", modelName, dto, prompt);
        ChatLanguageModel model = resolveModel(dto.getModel());

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            log.info(">>> Starting LLM call [{}] (continue): promptLength={}", modelName, prompt.length());
            Response<AiMessage> response = model.generate(userMessage);
            String content = response.content().text().trim();

            logModelCallResult("Continue", modelName, response, content);
            return WritingResultVO.builder()
                    .content(content)
                    .tokens(response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : null)
                    .wordCount(content.length())
                    .model(modelName)
                    .build();
        } catch (Exception e) {
            log.error("AI writing continuation failed: model={}, error={}", modelName, e.getMessage(), e);
            throw new RuntimeException("AI writing continuation failed: " + e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    @Cacheable(value = "writingTemplates")
    public List<WritingTemplateVO> getTemplates() {
        log.info("Getting the list of writing templates (cache miss, rebuilding)");
        List<WritingTemplateVO> templates = new ArrayList<>();

        templates.add(WritingTemplateVO.builder()
                .id("tech-solution")
                .name("Technical Proposal")
                .description("For writing technical proposal documents, with a complete structure covering background analysis, solution design, technology selection, and implementation plan")
                .category("Technical Documentation")
                .prompt("Please write a technical proposal covering the following sections:\nI. Project Background and Goals\nII. Current Technical State Analysis\nIII. Solution Design\nIV. Technology Selection Rationale\nV. Implementation Plan\nVI. Risk Assessment and Mitigation")
                .suggestedContentType("article")
                .suggestedStyle("technical")
                .build());

        templates.add(WritingTemplateVO.builder()
                .id("project-report")
                .name("Project Report")
                .description("For project summary reports, covering project overview, execution process, outcome analysis, and lessons learned")
                .category("Work Reports")
                .prompt("Please write a project summary report covering the following sections:\nI. Project Overview\nII. Execution Process\nIII. Key Outcomes\nIV. Issues and Challenges\nV. Lessons and Reflections\nVI. Next Steps")
                .suggestedContentType("report")
                .suggestedStyle("formal")
                .build());

        templates.add(WritingTemplateVO.builder()
                .id("prd")
                .name("Product Requirements Document (PRD)")
                .description("For product requirements documents, covering requirement background, feature descriptions, user scenarios, and acceptance criteria")
                .category("Product Documentation")
                .prompt("Please write a Product Requirements Document (PRD) covering the following sections:\nI. Requirement Background and Goals\nII. Target User Analysis\nIII. Core Feature Description\nIV. User Flow\nV. Interaction Design Requirements\nVI. Acceptance Criteria")
                .suggestedContentType("documentation")
                .suggestedStyle("technical")
                .build());

        templates.add(WritingTemplateVO.builder()
                .id("api-doc")
                .name("API Documentation")
                .description("For writing API documentation, covering interface descriptions, request parameters, response formats, and sample code")
                .category("Technical Documentation")
                .prompt("Please write API documentation covering the following sections:\nI. API Overview\nII. Authentication\nIII. Request Format\nIV. Response Format\nV. Detailed Description of Each Endpoint (with request and response examples)\nVI. Error Codes")
                .suggestedContentType("documentation")
                .suggestedStyle("technical")
                .build());

        templates.add(WritingTemplateVO.builder()
                .id("weekly-report")
                .name("Weekly/Status Report")
                .description("For weekly reports or status updates, covering this week's work, next week's plan, and issue feedback")
                .category("Work Reports")
                .prompt("Please write a weekly status report covering the following sections:\nI. Work Completed This Week\nII. Progress on Key Initiatives\nIII. Issues Encountered and Solutions\nIV. Plan for Next Week\nV. Items Needing Coordination")
                .suggestedContentType("report")
                .suggestedStyle("formal")
                .build());

        templates.add(WritingTemplateVO.builder()
                .id("meeting-minutes")
                .name("Meeting Minutes")
                .description("For meeting minutes, covering meeting information, discussion content, resolutions, and action items")
                .category("Work Reports")
                .prompt("Please write meeting minutes covering the following sections:\nI. Basic Meeting Information (time, location, attendees)\nII. Meeting Agenda\nIII. Key Discussion Points\nIV. Resolutions\nV. Action Items and Owners\nVI. Time of the Next Meeting")
                .suggestedContentType("email")
                .suggestedStyle("formal")
                .build());

        templates.add(WritingTemplateVO.builder()
                .id("announcement")
                .name("Announcement/Notice")
                .description("For internal company announcements or notices, covering the announcement topic, specific content, and execution requirements")
                .category("Administrative Documents")
                .prompt("Please write an announcement/notice covering the following sections:\nI. Announcement Title\nII. Reason for the Announcement\nIII. Description of Specific Matters\nIV. Execution Requirements or Schedule\nV. Contact Information")
                .suggestedContentType("announcement")
                .suggestedStyle("formal")
                .build());

        templates.add(WritingTemplateVO.builder()
                .id("email-template")
                .name("Email Template")
                .description("For writing work emails, covering the subject, greeting, body, and closing")
                .category("Daily Communication")
                .prompt("Please write a work email including the following elements:\n1. A clear, specific subject line\n2. An appropriate greeting\n3. A concise description of the matter\n4. Necessary background information\n5. Clear action items or expectations\n6. A professional closing and signature")
                .suggestedContentType("email")
                .suggestedStyle("formal")
                .build());

        return templates;
    }

    /**
     * Build the writing prompt
     *
     * @param dto writing request parameters
     * @return the complete prompt string
     */
    private String buildWritingPrompt(WritingRequestDTO dto) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional document writing assistant. Please write based on the following requirements.\n\n");

        // Content type description
        String contentType = dto.getContentType() != null ? dto.getContentType() : "article";
        String contentTypeDesc = switch (contentType) {
            case "report" -> "This is a report; it should have a clear structure, detailed data, and thorough argumentation";
            case "documentation" -> "This is technical documentation; it should be accurate, standardized, and actionable, including code examples";
            case "email" -> "This is an email; it should be polite, concise, and highlight the key points";
            case "announcement" -> "This is an announcement; it should be formal, rigorous, and clearly stated";
            default -> "This is an article; it should have a clear topic, clear logic, and fluent expression";
        };
        prompt.append(contentTypeDesc).append(".\n\n");

        // Style description
        String style = dto.getStyle() != null ? dto.getStyle() : "formal";
        String styleDesc = switch (style) {
            case "casual" -> "Please use a relaxed, casual style, avoiding overly formal phrasing";
            case "technical" -> "Please use a professional technical style, using technical terminology appropriately, with an emphasis on accuracy and rigor";
            case "creative" -> "Please use creative phrasing, without being bound by conventional format";
            case "academic" -> "Please use an academic paper style, with an emphasis on rigorous argumentation and proper citation";
            default -> "Please use a formal, standardized style with professional, appropriate phrasing";
        };
        prompt.append(styleDesc).append(".\n");

        // Tone description
        if (dto.getTone() != null && !dto.getTone().isEmpty()) {
            String toneDesc = switch (dto.getTone().toLowerCase()) {
                case "enthusiastic" -> "Use an enthusiastic, positive, and engaging tone";
                case "serious" -> "Use a serious, earnest tone, emphasizing authority";
                case "friendly" -> "Use a friendly, approachable tone that connects with the reader";
                case "authoritative" -> "Use an authoritative, professional tone that demonstrates expertise";
                default -> "Use a calm, neutral tone with objective statements";
            };
            prompt.append(toneDesc).append(".\n");
        }

        // Word count description
        int length = (dto.getLength() != null && dto.getLength() > 0) ? dto.getLength() : 800;
        prompt.append("Word count requirement: approximately ").append(length).append(" words.\n\n");

        // Topic and requirements
        prompt.append("Writing topic: ").append(dto.getTopic()).append("\n");
        if (dto.getRequirements() != null && !dto.getRequirements().isEmpty()) {
            prompt.append("Writing requirements: ").append(dto.getRequirements()).append("\n");
        }

        // Existing content (used for expand, optimize, and continue scenarios)
        if (dto.getExistingContent() != null && !dto.getExistingContent().isEmpty()) {
            prompt.append("\nReference content:\n```\n")
                    .append(dto.getExistingContent())
                    .append("\n```\n");
        }

        prompt.append("\nPlease output the writing result directly, without adding any extra explanation. Use Markdown format to organize the content.");
        return prompt.toString();
    }

    /**
     * Resolve a ChatLanguageModel by model name
     *
     * @param modelName the model name; the default model is used if empty
     * @return the ChatLanguageModel instance
     */
    private ChatLanguageModel resolveModel(String modelName) {
        if (modelName != null && !modelName.isEmpty()) {
            return modelProvider.getModel(modelName);
        }
        return modelProvider.getDefaultModel();
    }

    /**
     * Log the LLM call parameters
     *
     * @param action    operation name (generate/expand/optimize/continue)
     * @param modelName model name
     * @param dto       request parameters
     * @param prompt    the complete prompt text
     */
    private void logModelCallParams(String action, String modelName, WritingRequestDTO dto, String prompt) {
        log.info("══════ AI Writing [{}] - Model Call Parameters ══════", action);
        log.info("  Model name: {}", modelName);
        log.info("  Writing topic: {}", dto.getTopic());
        log.info("  Content type: {}", dto.getContentType() != null ? dto.getContentType() : "article (default)");
        log.info("  Writing style: {}", dto.getStyle() != null ? dto.getStyle() : "formal (default)");
        log.info("  Tone:     {}", dto.getTone() != null ? dto.getTone() : "neutral (default)");
        log.info("  Desired word count: {}", (dto.getLength() != null && dto.getLength() > 0) ? dto.getLength() : 800);
        log.info("  Writing requirements: {}", dto.getRequirements() != null && !dto.getRequirements().isEmpty()
                ? truncateForLog(dto.getRequirements(), 200) : "None");
        if (dto.getExistingContent() != null && !dto.getExistingContent().isEmpty()) {
            log.info("  Existing content length: {} characters", dto.getExistingContent().length());
        }
        log.info("  Total prompt length: {} characters", prompt.length());
        log.info("══════════════════════════════════════════");
    }

    /**
     * Log the LLM call result
     *
     * @param action    operation name
     * @param modelName model name
     * @param response  model response
     * @param content   response content
     */
    private void logModelCallResult(String action, String modelName, Response<AiMessage> response, String content) {
        Integer inputTokens = response.tokenUsage() != null ? response.tokenUsage().inputTokenCount() : null;
        Integer outputTokens = response.tokenUsage() != null ? response.tokenUsage().outputTokenCount() : null;
        Integer totalTokens = response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : null;
        log.info("<<< LLM call completed [{}]: model={}, output length={} chars, " +
                        "inputTokens={}, outputTokens={}, totalTokens={}",
                action, modelName, content.length(),
                inputTokens, outputTokens, totalTokens);
    }

    /**
     * Truncate log text (to avoid overly long log lines)
     *
     * @param text     the original text
     * @param maxChars maximum character count
     * @return the truncated text
     */
    private String truncateForLog(String text, int maxChars) {
        if (text == null) return "null";
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars) + "... (total length: " + text.length() + " characters)";
    }

    /**
     * Get the style description (used in the optimize scenario)
     *
     * @param style style identifier
     * @return the style description
     */
    private String getStyleDescription(String style) {
        if (style == null || style.isEmpty()) {
            return "Improve the article's overall readability and professionalism";
        }
        return switch (style.toLowerCase()) {
            case "casual" -> "Use relaxed, easy-to-read phrasing to make the article friendlier";
            case "technical" -> "Use more precise, professional technical terminology to improve technical accuracy";
            case "creative" -> "Use more creative and engaging phrasing";
            case "academic" -> "Use more rigorous academic phrasing to strengthen the argumentative logic";
            default -> "Improve the article's overall readability and professionalism";
        };
    }
}
