package com.knowledge.base.graph.repository;

import com.knowledge.base.graph.entity.node.KnowledgeEntityNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Knowledge entity graph repository
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Repository
public interface KnowledgeEntityRepository extends Neo4jRepository<KnowledgeEntityNode, String> {

    /**
     * Fuzzy-searches entities by name
     */
    @Query("MATCH (e:KnowledgeEntity) WHERE e.name CONTAINS $keyword OR any(a IN coalesce(e.aliases, []) WHERE a CONTAINS $keyword) RETURN e ORDER BY e.updatedAt DESC LIMIT $limit")
    List<KnowledgeEntityNode> searchByName(@Param("keyword") String keyword, @Param("limit") Integer limit);

    /**
     * Queries entities by type
     */
    @Query("MATCH (e:KnowledgeEntity) WHERE e.type = $type RETURN e ORDER BY e.updatedAt DESC LIMIT $limit")
    List<KnowledgeEntityNode> findByType(@Param("type") String type, @Param("limit") Integer limit);

    /**
     * Queries an entity's neighboring entities (within 1 hop)
     */
    @Query("MATCH (e:KnowledgeEntity {name: $name})-[r:DEPENDS_ON|USES|CONFIGURES|HAS_PART|RELATED_TO]-(neighbor:KnowledgeEntity) RETURN DISTINCT neighbor, type(r) AS relation ORDER BY neighbor.updatedAt DESC LIMIT $limit")
    List<KnowledgeEntityNode> findNeighbors(@Param("name") String name, @Param("limit") Integer limit);

    /**
     * Queries the list of relationship types between two entities
     */
    @Query("MATCH (a:KnowledgeEntity {name: $source})-[r]->(b:KnowledgeEntity {name: $target}) RETURN type(r) AS relationType")
    List<String> findRelationshipsBetween(@Param("source") String source, @Param("target") String target);

    /**
     * Queries document chunks that mention a specified entity
     */
    @Query("MATCH (e:KnowledgeEntity {name: $name})<-[:MENTIONS]-(c:DocumentChunk)-[:HAS_CHUNK]->(d:KnowledgeDocument) WHERE d.status = 1 RETURN c.content AS content, c.heading AS heading, d.title AS docTitle, d.docId AS docId, e.name AS entityName ORDER BY c.chunkIndex LIMIT $limit")
    List<EntityChunkMapping> findMentioningChunks(@Param("name") String name, @Param("limit") Integer limit);

    /**
     * Entity count statistics
     */
    @Query("MATCH (e:KnowledgeEntity) RETURN e.type AS type, count(e) AS count ORDER BY count DESC")
    List<EntityTypeStat> countByType();
}
