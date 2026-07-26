-- =====================================================
-- Migration script: add icon column to the kb_team table
-- Applicable scenario: upgrading an existing database
-- =====================================================

SET NAMES utf8mb4;
USE `kb_user`;

-- Add the icon column
ALTER TABLE `kb_team`
    ADD COLUMN `icon` VARCHAR(50) DEFAULT NULL COMMENT 'Team icon identifier'
    AFTER `description`;

-- Initialize icons for existing team data
UPDATE `kb_team` SET `icon` = 'tech' WHERE `team_code` = 'TECH_CENTER';
UPDATE `kb_team` SET `icon` = 'product' WHERE `team_code` = 'PRODUCT_CENTER';
UPDATE `kb_team` SET `icon` = 'ops' WHERE `team_code` = 'OPS_CENTER';
UPDATE `kb_team` SET `icon` = 'admin' WHERE `team_code` = 'ADMIN_CENTER';
UPDATE `kb_team` SET `icon` = 'backend' WHERE `team_code` = 'BACKEND_TEAM';
UPDATE `kb_team` SET `icon` = 'frontend' WHERE `team_code` = 'FRONTEND_TEAM';
UPDATE `kb_team` SET `icon` = 'qa' WHERE `team_code` = 'QA_TEAM';

SELECT 'kb_team table icon column added!' AS message;
