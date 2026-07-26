package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.dto.DocumentProcessDTO;
import com.knowledge.base.ai.service.AiDocumentService;
import com.knowledge.base.ai.vo.DocumentProcessVO;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI document processing controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
@Tag(name = "AI Document Processing", description = "AI document processing related APIs")
public class AiDocumentController {

    private final AiDocumentService aiDocumentService;

    /**
     * Generate a document summary
     *
     * @param file    document file
     * @param request HTTP request
     * @return summary result
     */
    @PostMapping("/summary")
    @Operation(summary = "Generate document summary", description = "Use AI to generate a document summary")
    public Result<DocumentProcessVO> generateSummary(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        DocumentProcessVO result = aiDocumentService.generateSummary(file, userId);
        return Result.success(result);
    }

    /**
     * Generate a document outline
     *
     * @param file    document file
     * @param request HTTP request
     * @return outline result
     */
    @PostMapping("/outline")
    @Operation(summary = "Generate document outline", description = "Use AI to generate a document outline")
    public Result<DocumentProcessVO> generateOutline(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        DocumentProcessVO result = aiDocumentService.generateOutline(file, userId);
        return Result.success(result);
    }

    /**
     * Expand content
     *
     * @param dto     processing request
     * @param request HTTP request
     * @return expansion result
     */
    @PostMapping("/expand")
    @Operation(summary = "Expand content", description = "Use AI to expand document content")
    public Result<DocumentProcessVO> expandContent(
            @Valid @RequestBody DocumentProcessDTO dto,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        DocumentProcessVO result = aiDocumentService.expandContent(dto, userId);
        return Result.success(result);
    }

    /**
     * Improve wording
     *
     * @param dto     processing request
     * @param request HTTP request
     * @return improvement result
     */
    @PostMapping("/optimize")
    @Operation(summary = "Improve wording", description = "Use AI to improve document wording")
    public Result<DocumentProcessVO> optimizeContent(
            @Valid @RequestBody DocumentProcessDTO dto,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        DocumentProcessVO result = aiDocumentService.optimizeContent(dto, userId);
        return Result.success(result);
    }

    /**
     * Generate a summary in streaming mode
     *
     * @param file    document file
     * @param request HTTP request
     * @return SSE event stream
     */
    @PostMapping("/summary/stream")
    @Operation(summary = "Generate summary (streaming)", description = "Use AI to generate a document summary in streaming mode")
    public SseEmitter generateSummaryStream(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        return aiDocumentService.generateSummaryStream(file, userId);
    }

    /**
     * Generate an outline in streaming mode
     *
     * @param file    document file
     * @param request HTTP request
     * @return SSE event stream
     */
    @PostMapping("/outline/stream")
    @Operation(summary = "Generate outline (streaming)", description = "Use AI to generate a document outline in streaming mode")
    public SseEmitter generateOutlineStream(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        return aiDocumentService.generateOutlineStream(file, userId);
    }

    /**
     * Generate a summary from provided content (non-streaming)
     *
     * @param dto     processing request containing the document content
     * @param request HTTP request
     * @return summary result
     */
    @PostMapping("/summary/content")
    @Operation(summary = "Generate summary from content", description = "Pass in document content and use AI to generate a summary")
    public Result<DocumentProcessVO> generateSummaryByContent(
            @Valid @RequestBody DocumentProcessDTO dto,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        DocumentProcessVO result = aiDocumentService.generateSummaryByContent(dto, userId);
        return Result.success(result);
    }

    /**
     * Generate a summary from provided content in streaming mode
     *
     * @param dto     processing request containing the document content
     * @param request HTTP request
     * @return SSE event stream
     */
    @PostMapping("/summary/content/stream")
    @Operation(summary = "Generate summary from content (streaming)", description = "Pass in document content and use AI to generate a summary in streaming mode")
    public SseEmitter generateSummaryByContentStream(
            @Valid @RequestBody DocumentProcessDTO dto,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        return aiDocumentService.generateSummaryByContentStream(dto, userId);
    }

}
