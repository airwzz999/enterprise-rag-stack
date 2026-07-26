-- =====================================================
-- kb_graph database - knowledge graph service
-- =====================================================

SET NAMES utf8mb4;
USE `kb_graph`;
SET FOREIGN_KEY_CHECKS = 0;

-- Graph node table (stored in MySQL; Neo4j is the primary store)
DROP TABLE IF EXISTS `kb_graph_node`;
CREATE TABLE `kb_graph_node` (
  `id` BIGINT NOT NULL COMMENT 'Node ID',
  `node_type` VARCHAR(50) NOT NULL COMMENT 'Node type: document/category/tag/user/concept',
  `node_name` VARCHAR(200) NOT NULL COMMENT 'Node name',
  `properties` JSON DEFAULT NULL COMMENT 'Node properties',
  `labels` VARCHAR(500) DEFAULT NULL COMMENT 'Node labels',
  `weight` INT DEFAULT 1 COMMENT 'Weight',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  KEY `idx_node_type` (`node_type`),
  KEY `idx_node_name` (`node_name`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Graph node table';

-- Graph edge table (stored in MySQL; Neo4j is the primary store)
DROP TABLE IF EXISTS `kb_graph_edge`;
CREATE TABLE `kb_graph_edge` (
  `id` BIGINT NOT NULL COMMENT 'Edge ID',
  `source_id` BIGINT NOT NULL COMMENT 'Source node ID',
  `target_id` BIGINT NOT NULL COMMENT 'Target node ID',
  `relationship_type` VARCHAR(50) NOT NULL COMMENT 'Relationship type',
  `relationship_name` VARCHAR(100) DEFAULT NULL COMMENT 'Relationship name',
  `properties` JSON DEFAULT NULL COMMENT 'Edge properties',
  `weight` DECIMAL(5,2) DEFAULT 1.0 COMMENT 'Weight',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_target_relation` (`source_id`, `target_id`, `relationship_type`),
  KEY `idx_source_id` (`source_id`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_relationship_type` (`relationship_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Graph edge table';

-- Graph community table
DROP TABLE IF EXISTS `kb_graph_community`;
CREATE TABLE `kb_graph_community` (
  `id` BIGINT NOT NULL COMMENT 'Community ID',
  `community_name` VARCHAR(100) NOT NULL COMMENT 'Community name',
  `node_count` INT NOT NULL DEFAULT 0 COMMENT 'Node count',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Description',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Graph community table';

-- Community-node association table
DROP TABLE IF EXISTS `kb_graph_community_node`;
CREATE TABLE `kb_graph_community_node` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `community_id` BIGINT NOT NULL COMMENT 'Community ID',
  `node_id` BIGINT NOT NULL COMMENT 'Node ID',
  `node_type` VARCHAR(50) DEFAULT NULL COMMENT 'Node type',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_community_node` (`community_id`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Community-node association table';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_graph database tables created!' AS message;
SELECT 'Note: the knowledge graph primarily uses Neo4j for storage; the MySQL tables serve as a supplement and backup.' AS note;
