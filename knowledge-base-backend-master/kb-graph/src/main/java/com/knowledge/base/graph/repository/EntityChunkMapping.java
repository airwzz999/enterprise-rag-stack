package com.knowledge.base.graph.repository;

/**
 * Entity-chunk mapping (returned by KnowledgeEntityRepository.findMentioningChunks)
 */
public interface EntityChunkMapping {

    String getContent();

    String getHeading();

    String getDocTitle();

    Long getDocId();

    String getEntityName();
}
