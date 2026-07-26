package com.knowledge.base.graph.repository;

/**
 * Entity type statistics (returned by KnowledgeEntityRepository.countByType)
 */
public interface EntityTypeStat {

    String getType();

    Long getCount();
}
