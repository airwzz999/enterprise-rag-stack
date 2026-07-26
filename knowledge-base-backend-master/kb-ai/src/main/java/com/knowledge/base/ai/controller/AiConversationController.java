package com.knowledge.base.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.ai.service.AiConversationService;
import com.knowledge.base.ai.vo.ConversationVO;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI conversation management controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
@Tag(name = "AI Conversation Management", description = "AI conversation management related APIs")
public class AiConversationController {

    private final AiConversationService conversationService;

    /**
     * Create a new conversation
     *
     * @param body    request body (contains title)
     * @param request HTTP request
     * @return the newly created conversation
     */
    @PostMapping
    @Operation(summary = "Create a new conversation", description = "Create a new AI conversation")
    public Result<ConversationVO> createConversation(@RequestBody Map<String, String> body,
                                                       HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        String title = body.getOrDefault("title", "New Conversation");
        Long conversationId = conversationService.createConversation(title, userId);
        ConversationVO conversation = conversationService.getConversation(conversationId, userId);
        return Result.success(conversation);
    }

    /**
     * Get the list of conversations
     *
     * @param current current page
     * @param size    page size
     * @param request HTTP request
     * @return list of conversations
     */
    @GetMapping("/list")
    @Operation(summary = "Get conversation list", description = "Get the list of conversations for the current user")
    public Result<IPage<ConversationVO>> listConversations(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        IPage<ConversationVO> conversations = conversationService.listConversations(userId, current, size);
        return Result.success(conversations);
    }

    /**
     * Get conversation details
     *
     * @param id      conversation ID
     * @param request HTTP request
     * @return conversation details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get conversation details", description = "Get detailed information about a conversation")
    public Result<ConversationVO> getConversation(@PathVariable Long id,
                                                   HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        ConversationVO conversation = conversationService.getConversation(id, userId);
        return Result.success(conversation);
    }

    /**
     * Delete a conversation
     *
     * @param id      conversation ID
     * @param request HTTP request
     * @return whether the operation succeeded
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete conversation", description = "Delete the specified conversation")
    public Result<Boolean> deleteConversation(@PathVariable Long id,
                                               HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        boolean success = conversationService.deleteConversation(id, userId);
        return Result.success(success);
    }
}
