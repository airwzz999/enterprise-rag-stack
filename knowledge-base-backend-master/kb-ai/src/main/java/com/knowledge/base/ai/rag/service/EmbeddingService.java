package com.knowledge.base.ai.rag.service;

import java.util.List;

/**
 * Embedding service interface
 *
 * <p>Converts text into vector embeddings, supporting both single and batch
 * operations, with Redis caching support.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface EmbeddingService {

    /**
     * Generate a vector embedding for a single piece of text
     *
     * @param text the input text
     * @return a 1024-dimensional float vector
     */
    float[] embed(String text);

    /**
     * Generate vector embeddings in batch
     *
     * @param texts list of input texts
     * @return the corresponding list of vectors
     */
    List<float[]> embedBatch(List<String> texts);
}
