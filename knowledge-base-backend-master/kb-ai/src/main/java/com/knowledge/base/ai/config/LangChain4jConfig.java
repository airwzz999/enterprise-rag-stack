package com.knowledge.base.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j configuration class (retained; ChatLanguageModel is created on demand by ModelProvider)
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class LangChain4jConfig {
    // ChatLanguageModel bean creation has moved to ModelProvider,
    // because OpenAiChatModel enforces a non-empty API Key at build time.
    // ModelProvider builds it on demand when a request arrives, allowing the
    // application to start normally even without an API Key configured.
}
