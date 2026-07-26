package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.dto.FeedbackDTO;
import com.knowledge.base.ai.service.AiFeedbackService;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI feedback controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/feedback")
@RequiredArgsConstructor
@Tag(name = "AI Feedback", description = "AI feedback related APIs")
public class AiFeedbackController {

    private final AiFeedbackService feedbackService;

    /**
     * Submit feedback
     *
     * @param feedbackDTO feedback information
     * @param request     HTTP request
     * @return whether the operation succeeded
     */
    @PostMapping
    @Operation(summary = "Submit feedback", description = "Submit feedback on AI usage")
    public Result<Boolean> submitFeedback(
            @Valid @RequestBody FeedbackDTO feedbackDTO,
            HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        Boolean success = feedbackService.submitFeedback(feedbackDTO, userId);
        return Result.success(success);
    }

    /**
     * Get the list of feedback for the current user
     *
     * @param request HTTP request
     * @return list of feedback
     */
    @GetMapping("/list")
    @Operation(summary = "Get feedback list", description = "Get the feedback list for the current user")
    public Result<?> getUserFeedbacks(HttpServletRequest request) {
        Long userId = UserContextUtil.getUserIdFromHeader(request);
        return Result.success(feedbackService.getUserFeedbacks(userId));
    }

}
