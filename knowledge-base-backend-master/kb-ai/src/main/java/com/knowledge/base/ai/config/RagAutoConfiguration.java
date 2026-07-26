package com.knowledge.base.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * RAG auto-configuration
 *
 * <p>Automatically scans and loads RAG components when rag.enabled=true.
 * When rag.enabled=false, RAG beans are not created, and the chat feature
 * falls back to pure LLM mode.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.knowledge.base.ai.rag")
public class RagAutoConfiguration {

    public RagAutoConfiguration() {
        log.info("✅ RAG feature enabled, loading RAG components...");
    }
}
