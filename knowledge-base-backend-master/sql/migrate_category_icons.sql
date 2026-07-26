-- =====================================================
-- Migration script: migrate category icons from emoji to icon identifier keys
-- Applicable scenario: upgrading an existing database, mapping old emoji icons to new icon keys
-- =====================================================

SET NAMES utf8mb4;
USE `kb_document`;

-- Top-level category mapping
UPDATE `kb_category` SET `category_icon` = 'tech'         WHERE `category_icon` = '💻';
UPDATE `kb_category` SET `category_icon` = 'product'      WHERE `category_icon` = '📦';
UPDATE `kb_category` SET `category_icon` = 'business'     WHERE `category_icon` = '📋';
UPDATE `kb_category` SET `category_icon` = 'hr'           WHERE `category_icon` = '👥';
UPDATE `kb_category` SET `category_icon` = 'finance'      WHERE `category_icon` = '💰';
UPDATE `kb_category` SET `category_icon` = 'marketing'    WHERE `category_icon` = '📈';
UPDATE `kb_category` SET `category_icon` = 'legal'        WHERE `category_icon` = '⚖️';
UPDATE `kb_category` SET `category_icon` = 'training'     WHERE `category_icon` = '📚';

-- Technical documentation subcategory mapping
UPDATE `kb_category` SET `category_icon` = 'backend'      WHERE `category_icon` = '🔧';
UPDATE `kb_category` SET `category_icon` = 'frontend'     WHERE `category_icon` = '🎨';
UPDATE `kb_category` SET `category_icon` = 'database'     WHERE `category_icon` = '🗄️';
UPDATE `kb_category` SET `category_icon` = 'devops'       WHERE `category_icon` = '🚀';
UPDATE `kb_category` SET `category_icon` = 'architecture' WHERE `category_icon` = '🏗️';

-- Product documentation subcategory mapping
UPDATE `kb_category` SET `category_icon` = 'requirement'  WHERE `category_icon` = '📝';
UPDATE `kb_category` SET `category_icon` = 'design'       WHERE `category_icon` = '🎭';
UPDATE `kb_category` SET `category_icon` = 'planning'     WHERE `category_icon` = '🎯';
UPDATE `kb_category` SET `category_icon` = 'competitive'  WHERE `category_icon` = '🔍';

-- Default emoji → tech
UPDATE `kb_category` SET `category_icon` = 'tech'         WHERE `category_icon` = '📁';

SELECT 'Category icon migration complete!' AS message;
