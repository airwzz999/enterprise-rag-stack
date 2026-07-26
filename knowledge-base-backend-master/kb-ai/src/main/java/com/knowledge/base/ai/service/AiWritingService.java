package com.knowledge.base.ai.service;

import com.knowledge.base.ai.dto.WritingRequestDTO;
import com.knowledge.base.ai.vo.WritingResultVO;
import com.knowledge.base.ai.vo.WritingTemplateVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI writing service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface AiWritingService {

    /**
     * Generate writing content based on a topic and requirements
     *
     * @param dto    writing request parameters
     * @param userId user ID
     * @return writing result
     */
    WritingResultVO generate(WritingRequestDTO dto, Long userId);

    /**
     * Generate writing content in streaming mode (SSE)
     *
     * @param dto    writing request parameters
     * @param userId user ID
     * @return SSE event emitter
     */
    SseEmitter generateStream(WritingRequestDTO dto, Long userId);

    /**
     * Expand existing content
     *
     * @param dto    writing request parameters (must include existingContent)
     * @param userId user ID
     * @return writing result
     */
    WritingResultVO expand(WritingRequestDTO dto, Long userId);

    /**
     * Optimize/polish existing content
     *
     * @param dto    writing request parameters (must include existingContent)
     * @param userId user ID
     * @return writing result
     */
    WritingResultVO optimize(WritingRequestDTO dto, Long userId);

    /**
     * Continue writing from existing content
     *
     * @param dto    writing request parameters (must include existingContent)
     * @param userId user ID
     * @return writing result
     */
    WritingResultVO continueWriting(WritingRequestDTO dto, Long userId);

    /**
     * Get the list of writing prompt templates
     *
     * @return list of templates
     */
    List<WritingTemplateVO> getTemplates();
}
