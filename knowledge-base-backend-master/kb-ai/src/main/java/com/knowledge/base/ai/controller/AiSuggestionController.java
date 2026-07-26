package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.config.AiSuggestionProperties;
import com.knowledge.base.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI suggestion controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/suggestions")
@Tag(name = "AI Suggestions", description = "AI suggestion related APIs")
public class AiSuggestionController {

    private final AiSuggestionProperties suggestionProperties;

    public AiSuggestionController(AiSuggestionProperties suggestionProperties) {
        this.suggestionProperties = suggestionProperties;
    }

    /**
     * Get the list of AI suggested questions
     *
     * @return list of suggested questions
     */
    @GetMapping
    @Operation(summary = "Get AI suggestions", description = "Get AI quick questions and suggestions")
    public Result<List<String>> getSuggestions() {
        List<String> suggestions = suggestionProperties.getItems();
        return Result.success(suggestions);
    }
}
