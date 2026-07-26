-- =====================================================
-- kb_user database - personal profile feature upgrade
-- 1. Add the remark column (personal bio)
-- 2. Create the kb_document cross-database view (for user statistics queries)
-- =====================================================

SET NAMES utf8mb4;
USE `kb_user`;

-- =====================================================
-- 1. Add the remark column
-- =====================================================
-- MySQL 8.0 supports IF NOT EXISTS; on older versions please check manually
ALTER TABLE `kb_user`
    ADD COLUMN IF NOT EXISTS `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Personal bio / remark'
    AFTER `position`;

-- =====================================================
-- 2. Create the kb_document cross-database view (read-only)
--    Used to query the number of documents published and likes received by a user
-- =====================================================
DROP VIEW IF EXISTS `kb_document`;
CREATE VIEW `kb_document` AS
SELECT
    id, title, author_id, author_name, category_id, status,
    view_count, like_count, favorite_count, comment_count,
    is_public, is_top, is_recommend, document_type, source,
    cover_image, tags, summary, content_id, content_length,
    sort, allow_comment, publish_time, created_at, updated_at,
    create_by, update_by, deleted
FROM kb_document.kb_document;

SELECT 'kb_user personal profile feature upgrade SQL executed!' AS message;
