package com.knowledge.base.graph.repository;

import com.knowledge.base.graph.entity.node.KnowledgeDocumentNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Knowledge document graph repository
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Repository
public interface KnowledgeDocumentRepository extends Neo4jRepository<KnowledgeDocumentNode, Long> {

    /**
     * Queries all published documents
     */
    @Query("MATCH (d:KnowledgeDocument) WHERE d.status = 1 RETURN d ORDER BY d.updatedAt DESC")
    List<KnowledgeDocumentNode> findAllPublished();

    /**
     * Queries published documents under a specified category
     */
    @Query("MATCH (d:KnowledgeDocument) WHERE d.categoryId = $categoryId AND d.status = 1 RETURN d")
    List<KnowledgeDocumentNode> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Searches published documents by title keyword
     */
    @Query("MATCH (d:KnowledgeDocument) WHERE d.status = 1 AND d.title CONTAINS $keyword RETURN d")
    List<KnowledgeDocumentNode> searchByTitle(@Param("keyword") String keyword);

    /**
     * Deletes the specified document and all its associations
     */
    @Query("MATCH (d:KnowledgeDocument {docId: $docId}) DETACH DELETE d")
    void deleteByDocId(@Param("docId") Long docId);

    /**
     * Queries the document count
     */
    @Query("MATCH (d:KnowledgeDocument) WHERE d.status = 1 RETURN count(d)")
    long countPublished();
}
