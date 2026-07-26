package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.dto.WritingRequestDTO;
import com.knowledge.base.ai.service.AiWritingService;
import com.knowledge.base.ai.vo.WritingResultVO;
import com.knowledge.base.ai.vo.WritingTemplateVO;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI writing controller
 *
 * <p>Provides AI writing related APIs, including content generation, expansion,
 * refinement, continuation, and template management.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/writing")
@RequiredArgsConstructor
@Tag(name = "AI Writing", description = "AI writing related APIs")
public class AiWritingController {

    private final AiWritingService aiWritingService;

    /**
     * Generate writing content based on a topic and requirements
     *
     * @param dto     writing request parameters
     * @param request HTTP request
     * @return writing result
     */
    @PostMapping("/generate")
    @Operation(summary = "Generate writing content", description = "Use AI to generate writing content based on the topic and requirement parameters")
    public Result<WritingResultVO> generate(
            @Valid @RequestBody WritingRequestDTO dto,
            HttpServletRequest request) {
        log.info("AI writing generation request: topic={}, contentType={}, style={}", dto.getTopic(), dto.getContentType(), dto.getStyle());
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        WritingResultVO result = aiWritingService.generate(dto, userId);
        return Result.success(result);
    }

    /**
     * Generate writing content in streaming mode (SSE)
     *
     * @param dto     writing request parameters
     * @param request HTTP request
     * @return SSE event stream
     */
    @PostMapping("/generate/stream")
    @Operation(summary = "Generate writing content (streaming)", description = "Generate writing content via streaming (SSE), returning results in real time")
    public SseEmitter generateStream(
            @Valid @RequestBody WritingRequestDTO dto,
            HttpServletRequest request) {
        log.info("AI writing streaming generation request: topic={}", dto.getTopic());
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        return aiWritingService.generateStream(dto, userId);
    }

    /**
     * Expand existing content
     *
     * @param dto     writing request parameters (must include existingContent)
     * @param request HTTP request
     * @return writing result
     */
    @PostMapping("/expand")
    @Operation(summary = "Expand content", description = "Expand existing content, enriching details and depth")
    public Result<WritingResultVO> expand(
            @Valid @RequestBody WritingRequestDTO dto,
            HttpServletRequest request) {
        log.info("AI writing expansion request: topic={}, existingContentLength={}",
                dto.getTopic(), dto.getExistingContent() != null ? dto.getExistingContent().length() : 0);
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        WritingResultVO result = aiWritingService.expand(dto, userId);
        return Result.success(result);
    }

    /**
     * Optimize/polish content
     *
     * @param dto     writing request parameters (must include existingContent)
     * @param request HTTP request
     * @return writing result
     */
    @PostMapping("/optimize")
    @Operation(summary = "Optimize and polish", description = "Optimize and polish existing content to improve the quality of expression")
    public Result<WritingResultVO> optimize(
            @Valid @RequestBody WritingRequestDTO dto,
            HttpServletRequest request) {
        log.info("AI writing optimization request: topic={}, existingContentLength={}",
                dto.getTopic(), dto.getExistingContent() != null ? dto.getExistingContent().length() : 0);
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        WritingResultVO result = aiWritingService.optimize(dto, userId);
        return Result.success(result);
    }

    /**
     * Continue writing content
     *
     * @param dto     writing request parameters (must include existingContent)
     * @param request HTTP request
     * @return writing result
     */
    @PostMapping("/continue")
    @Operation(summary = "Continue writing", description = "Continue writing from the end of existing content")
    public Result<WritingResultVO> continueWriting(
            @Valid @RequestBody WritingRequestDTO dto,
            HttpServletRequest request) {
        log.info("AI writing continuation request: topic={}, existingContentLength={}",
                dto.getTopic(), dto.getExistingContent() != null ? dto.getExistingContent().length() : 0);
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        WritingResultVO result = aiWritingService.continueWriting(dto, userId);
        return Result.success(result);
    }

    /**
     * Get writing prompt templates
     *
     * @return list of templates
     */
    @GetMapping("/templates")
    @Operation(summary = "Get writing templates", description = "Get the list of preset writing prompt templates for quickly starting to write")
    public Result<List<WritingTemplateVO>> getTemplates() {
        log.info("Getting the list of writing templates");
        List<WritingTemplateVO> templates = aiWritingService.getTemplates();
        return Result.success(templates);
    }
}
