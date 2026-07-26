package com.knowledge.base.ai.rag.service.impl;

import com.alibaba.fastjson2.JSON;
import com.knowledge.base.ai.config.RagProperties;
import com.knowledge.base.ai.rag.service.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Embedding service implementation
 *
 * <p>Uses LangChain4j's EmbeddingModel (OpenAI-compatible interface) to generate
 * vector embeddings. Supports Redis caching to reduce redundant embedding calls.
 * Both EmbeddingModel and RedisTemplate are optional dependencies; when not
 * configured, a zero vector is returned as a fallback.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    private final RagProperties ragProperties;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public EmbeddingServiceImpl(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    /** {@inheritDoc} */
    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[ragProperties.getEmbedding().getDimension()];
        }

        if (embeddingModel == null) {
            log.debug("EmbeddingModel unavailable, returning zero vector");
            return new float[ragProperties.getEmbedding().getDimension()];
        }

        // Check the cache
        if (ragProperties.getEmbedding().isCacheEnabled() && redisTemplate != null) {
            String cacheKey = buildCacheKey(text);
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return parseEmbedding(cached);
                }
            } catch (Exception e) {
                log.warn("Failed to read from Redis cache, skipping cache: {}", e.getMessage());
            }
        }

        // Call the embedding API
        float[] vector = callEmbedApi(text);

        // Write to the cache
        if (ragProperties.getEmbedding().isCacheEnabled() && redisTemplate != null) {
            String cacheKey = buildCacheKey(text);
            try {
                redisTemplate.opsForValue().set(
                        cacheKey,
                        JSON.toJSONString(vector),
                        ragProperties.getEmbedding().getCacheTtlSeconds(),
                        TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Failed to write to Redis cache: {}", e.getMessage());
            }
        }

        return vector;
    }

    /** {@inheritDoc} */
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (embeddingModel == null) {
            log.debug("EmbeddingModel unavailable, returning zero vectors for the batch");
            int dim = ragProperties.getEmbedding().getDimension();
            List<float[]> zeroVectors = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                zeroVectors.add(new float[dim]);
            }
            return zeroVectors;
        }

        int batchSize = ragProperties.getEmbedding().getBatchSize();
        List<float[]> allVectors = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<TextSegment> segments = new ArrayList<>();
            for (int j = i; j < end; j++) {
                segments.add(TextSegment.from(texts.get(j)));
            }

            try {
                Response<List<Embedding>> response = embeddingModel.embedAll(segments);
                List<Embedding> embeddings = response.content();
                for (Embedding embedding : embeddings) {
                    allVectors.add(embedding.vector());
                }
                log.debug("Batch embedding: {}/{} items, {} in this batch", end, texts.size(), end - i);
            } catch (Exception e) {
                log.error("Batch embedding failed (offset={}, count={}): {}", i, end - i, e.getMessage());
                // Fall back to single-item calls on failure
                for (int j = i; j < end; j++) {
                    try {
                        allVectors.add(embed(texts.get(j)));
                    } catch (Exception ex) {
                        log.error("Single-item embedding failed (index={}): {}", j, ex.getMessage());
                        allVectors.add(new float[ragProperties.getEmbedding().getDimension()]);
                    }
                }
            }
        }

        return allVectors;
    }

    private String buildCacheKey(String text) {
        return "rag:emb:" + DigestUtils.md5Hex(text);
    }

    private float[] parseEmbedding(String json) {
        return JSON.parseObject(json, float[].class);
    }

    private float[] callEmbedApi(String text) {
        Response<Embedding> response = embeddingModel.embed(TextSegment.from(text));
        return response.content().vector();
    }
}
