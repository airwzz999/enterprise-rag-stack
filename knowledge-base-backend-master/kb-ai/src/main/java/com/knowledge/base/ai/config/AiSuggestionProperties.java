package com.knowledge.base.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI suggestion configuration properties
 *
 * <p>Reads the ai.suggestions configuration from application.yml.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.suggestions")
public class AiSuggestionProperties {

    /**
     * List of quick-suggestion questions
     */
    private List<String> items = new ArrayList<>();
}
