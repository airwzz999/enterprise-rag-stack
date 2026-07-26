-- =====================================================
-- Migration script: add team_id column to the kb_document table
-- Purpose: support associating documents with a team space
-- =====================================================

SET NAMES utf8mb4;
USE `kb_document`;

ALTER TABLE `kb_document`
  ADD COLUMN `team_id` BIGINT DEFAULT NULL COMMENT 'Team space ID' AFTER `category_id`;

ALTER TABLE `kb_document`
  ADD INDEX `idx_team_id` (`team_id`);

SELECT 'team_id column added!' AS message;
