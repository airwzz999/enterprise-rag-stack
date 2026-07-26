package com.knowledge.base.ai.config;

import com.knowledge.base.ai.vo.ModelVO;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI model provider
 *
 * <p>Manages multiple large AI model instances, routing to the corresponding
 * ChatLanguageModel based on model name. ChatLanguageModel instances are lazily
 * created on demand, and only models with a configured API Key are registered.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class ModelProvider {

    // ==================== Qwen Configuration ====================

    @Value("${qwen.api-key}")
    private String qwenApiKey;

    @Value("${qwen.base-url}")
    private String qwenBaseUrl;

    @Value("${qwen.chat.options.model}")
    private String qwenModel;

    @Value("${qwen.chat.options.max-tokens}")
    private Integer qwenMaxTokens;

    @Value("${qwen.chat.options.temperature}")
    private Double qwenTemperature;

    // ==================== DeepSeek Configuration ====================

    @Value("${deepseek.api-key}")
    private String deepseekApiKey;

    @Value("${deepseek.base-url}")
    private String deepseekBaseUrl;

    @Value("${deepseek.model}")
    private String deepseekModel;

    @Value("${deepseek.max-tokens}")
    private Integer deepseekMaxTokens;

    @Value("${deepseek.temperature}")
    private Double deepseekTemperature;

    // ==================== Internal State ====================

    @Value("${ai.default-model:qwen}")
    private String defaultModel;

    /** Model instance cache (lazily loaded) */
    private final Map<String, ChatLanguageModel> instanceCache = new ConcurrentHashMap<>();

    /** Streaming model instance cache (lazily loaded) */
    private final Map<String, StreamingChatLanguageModel> streamingInstanceCache = new ConcurrentHashMap<>();

    /** Model availability (based on whether the API Key is configured) */
    private final Map<String, Boolean> modelAvailable = new LinkedHashMap<>();

    /** Model metadata */
    private final Map<String, ModelVO> modelInfoMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        checkAndRegister("qwen", "Qwen", "Alibaba Cloud large language model, supports multi-turn dialogue, text generation, and more", qwenApiKey);
        checkAndRegister("deepseek", "DeepSeek", "DeepSeek large language model, excels at code generation and deep reasoning", deepseekApiKey);

        if (modelInfoMap.isEmpty()) {
            log.error("❌ No AI model has an API Key configured! Please set at least the QWEN_API_KEY or DEEPSEEK_API_KEY environment variable");
        }

        if (modelAvailable.containsKey(defaultModel) && modelAvailable.get(defaultModel)) {
            log.info("✅ Default model '{}' is ready", defaultModel);
        } else {
            log.warn("⚠️ API Key for default model '{}' is not configured; please set the corresponding environment variable and restart", defaultModel);
        }
    }

    private void checkAndRegister(String key, String displayName, String description, String apiKey) {
        boolean available = apiKey != null && !apiKey.isEmpty();
        modelAvailable.put(key, available);
        if (available) {
            modelInfoMap.put(key, ModelVO.builder()
                    .key(key)
                    .displayName(displayName)
                    .description(description)
                    .isDefault(key.equals(defaultModel))
                    .build());
            log.info("✅ Model '{}' ({}) registered (API Key configured)", key, displayName);
        } else {
            log.warn("⚠️ API Key for model '{}' ({}) is not configured; this model is unavailable", key, displayName);
        }
    }

    /**
     * Lazily build a ChatLanguageModel instance (blocking)
     */
    private ChatLanguageModel buildModel(String modelName) {
        switch (modelName) {
            case "qwen":
                return OpenAiChatModel.builder()
                        .baseUrl(qwenBaseUrl)
                        .apiKey(qwenApiKey)
                        .modelName(qwenModel)
                        .maxTokens(qwenMaxTokens)
                        .temperature(qwenTemperature)
                        .build();
            case "deepseek":
                return OpenAiChatModel.builder()
                        .baseUrl(deepseekBaseUrl)
                        .apiKey(deepseekApiKey)
                        .modelName(deepseekModel)
                        .maxTokens(deepseekMaxTokens)
                        .temperature(deepseekTemperature)
                        .build();
            default:
                throw new IllegalStateException("Unknown model: " + modelName);
        }
    }

    /**
     * Lazily build a StreamingChatLanguageModel instance (streaming output)
     */
    private StreamingChatLanguageModel buildStreamingModel(String modelName) {
        switch (modelName) {
            case "qwen":
                return OpenAiStreamingChatModel.builder()
                        .baseUrl(qwenBaseUrl)
                        .apiKey(qwenApiKey)
                        .modelName(qwenModel)
                        .maxTokens(qwenMaxTokens)
                        .temperature(qwenTemperature)
                        .build();
            case "deepseek":
                return OpenAiStreamingChatModel.builder()
                        .baseUrl(deepseekBaseUrl)
                        .apiKey(deepseekApiKey)
                        .modelName(deepseekModel)
                        .maxTokens(deepseekMaxTokens)
                        .temperature(deepseekTemperature)
                        .build();
            default:
                throw new IllegalStateException("Unknown model: " + modelName);
        }
    }

    /**
     * Get a ChatLanguageModel instance by model name (lazily loaded)
     *
     * @param modelName the model name (qwen | deepseek)
     * @return the ChatLanguageModel instance
     * @throws IllegalStateException if the requested model has no API Key configured
     */
    public ChatLanguageModel getModel(String modelName) {
        String key = resolveModelKey(modelName);
        if (!modelAvailable.containsKey(key) || !modelAvailable.get(key)) {
            throw new IllegalStateException(
                    "Model '" + key + "' has no API Key configured and cannot be used. Please set the environment variable and restart the service.");
        }
        return instanceCache.computeIfAbsent(key, k -> {
            log.info("🔄 First call, building ChatLanguageModel: {}", k);
            return buildModel(k);
        });
    }

    /**
     * Get a StreamingChatLanguageModel instance by model name (streaming output, lazily loaded)
     *
     * @param modelName the model name (qwen | deepseek)
     * @return the StreamingChatLanguageModel instance
     * @throws IllegalStateException if the requested model has no API Key configured
     */
    public StreamingChatLanguageModel getStreamingModel(String modelName) {
        String key = resolveModelKey(modelName);
        if (!modelAvailable.containsKey(key) || !modelAvailable.get(key)) {
            throw new IllegalStateException(
                    "Model '" + key + "' has no API Key configured and cannot be used. Please set the environment variable and restart the service.");
        }
        return streamingInstanceCache.computeIfAbsent(key, k -> {
            log.info("🔄 First call, building StreamingChatLanguageModel: {}", k);
            return buildStreamingModel(k);
        });
    }

    /**
     * Get the default model
     * @throws IllegalStateException if no model is available
     */
    public ChatLanguageModel getDefaultModel() {
        if (modelInfoMap.isEmpty()) {
            throw new IllegalStateException(
                    "No AI model is available. Please set at least the QWEN_API_KEY or DEEPSEEK_API_KEY environment variable and restart the service.");
        }
        if (modelAvailable.containsKey(defaultModel) && modelAvailable.get(defaultModel)) {
            return getModel(defaultModel);
        }
        // Fallback: return the first available model
        String firstAvailable = modelAvailable.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No AI model is available"));
        log.warn("Default model '{}' is unavailable, falling back to: {}", defaultModel, firstAvailable);
        return getModel(firstAvailable);
    }

    /**
     * Get the default model name
     */
    public String getDefaultModelName() {
        return defaultModel;
    }

    /**
     * Get the list of all available model info (only returns models with a configured API Key)
     */
    public List<ModelVO> getAvailableModels() {
        return new ArrayList<>(modelInfoMap.values());
    }

    private String resolveModelKey(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return defaultModel;
        }
        return modelName.toLowerCase();
    }
}
