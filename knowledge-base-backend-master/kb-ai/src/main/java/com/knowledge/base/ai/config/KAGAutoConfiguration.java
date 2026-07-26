package com.knowledge.base.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * KAG auto-configuration
 *
 * <p>Automatically scans and loads KAG components when kag.enabled=true.
 * When kag.enabled=false, KAG beans are not created, and the Q&amp;A feature
 * falls back to pure RAG mode.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "kag.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.knowledge.base.ai.rag.kag")
public class KAGAutoConfiguration {

    public KAGAutoConfiguration() {
        log.info("KAG knowledge graph enhancement enabled, loading KAG components...");
    }
}
