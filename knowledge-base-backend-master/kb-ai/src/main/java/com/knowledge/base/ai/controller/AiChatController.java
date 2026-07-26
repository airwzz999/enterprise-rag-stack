package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.config.ModelProvider;
import com.knowledge.base.ai.dto.ChatRequestDTO;
import com.knowledge.base.ai.service.AiChatService;
import com.knowledge.base.ai.service.AiConversationService;
import com.knowledge.base.ai.vo.ChatResponseVO;
import com.knowledge.base.ai.vo.ModelVO;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI chat controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "AI chat related APIs")
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiConversationService conversationService;
    private final ModelProvider modelProvider;

    /**
     * Get the list of available AI models
     *
     * @return list of models
     */
    @GetMapping("/models")
    @Operation(summary = "Get AI model list", description = "Get the list of currently available large language models")
    public Result<List<ModelVO>> getModels() {
        List<ModelVO> models = modelProvider.getAvailableModels();
        return Result.success(models);
    }

    /**
     * AI chat
     *
     * @param requestDTO chat request
     * @param request    HTTP request
     * @return chat response
     */
    @PostMapping
    @Operation(summary = "AI chat", description = "Chat with the AI")
    public Result<ChatResponseVO> chat(@Validated @RequestBody ChatRequestDTO requestDTO,
                                        HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        ChatResponseVO response = aiChatService.chat(requestDTO, userId);
        return Result.success(response);
    }

    /**
     * AI streaming chat
     *
     * @param requestDTO chat request
     * @param request    HTTP request
     * @return SSE event stream
     */
    @PostMapping("/stream")
    @Operation(summary = "AI streaming chat", description = "Stream a chat conversation with the AI")
    public SseEmitter chatStream(@Validated @RequestBody ChatRequestDTO requestDTO,
                                  HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        return aiChatService.chatStream(requestDTO, userId);
    }

}
