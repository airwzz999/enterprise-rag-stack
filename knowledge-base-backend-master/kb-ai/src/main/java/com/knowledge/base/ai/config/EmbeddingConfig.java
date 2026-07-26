package com.knowledge.base.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Embedding model configuration
 *
 * <p>Creates the OpenAiEmbeddingModel bean using Qwen's text-embedding-v3 model.
 * Reuses the existing Qwen API Key and Base URL configuration,
 * following the same OpenAI-compatible interface pattern as ModelProvider.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class EmbeddingConfig {

    @Value("${qwen.api-key}")
    private String qwenApiKey;

    @Value("${qwen.base-url}")
    private String qwenBaseUrl;

    @Value("${rag.embedding.model:text-embedding-v3}")
    private String embeddingModel;

    /**
     * Create the Qwen EmbeddingModel bean
     *
     * <p>Only created when rag.enabled=true and an API Key is configured.
     * Uses @ConditionalOnExpression to ensure the bean is only registered when
     * the API Key is non-empty, avoiding a null return that would break dependency injection.</p>
     */
    @Bean
    @ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnExpression("'${qwen.api-key:}' != ''")
    public EmbeddingModel embeddingModel() {
        log.info("✅ Creating EmbeddingModel: model={}, provider=qwen", embeddingModel);
        return OpenAiEmbeddingModel.builder()
                .apiKey(qwenApiKey)
                .baseUrl(qwenBaseUrl)
                .modelName(embeddingModel)
                .build();
    }

    @PostConstruct
    public void init() {
        if (qwenApiKey != null && !qwenApiKey.isEmpty()) {
            log.info("✅ RAG embedding model is ready: model={}, dimension=1024", embeddingModel);
        } else {
            log.warn("⚠️ RAG embedding model unavailable: QWEN_API_KEY is not configured");
        }
    }
}
