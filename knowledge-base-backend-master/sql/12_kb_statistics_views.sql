-- =====================================================
-- kb_statistics database - cross-database views
-- Provides the statistics service transparent read-only access to operational data
-- =====================================================

SET NAMES utf8mb4;
USE `kb_statistics`;

-- Drop existing views if present (idempotent re-run)
DROP VIEW IF EXISTS `kb_document`;
DROP VIEW IF EXISTS `kb_user`;
DROP VIEW IF EXISTS `kb_comment`;
DROP VIEW IF EXISTS `kb_operation_log`;

-- =====================================================
-- View: kb_document (source: kb_document.kb_document)
-- =====================================================
CREATE VIEW `kb_document` AS
SELECT
    id, title, author_id, author_name, category_id, status,
    view_count, like_count, favorite_count, comment_count,
    is_public, is_top, is_recommend, document_type, source,
    cover_image, summary, sort, allow_comment, publish_time,
    created_at, updated_at, create_by, update_by, deleted
FROM kb_document.kb_document;

-- =====================================================
-- View: kb_user (source: kb_user.kb_user)
-- =====================================================
CREATE VIEW `kb_user` AS
SELECT
    id, username, real_name, avatar, status,
    email, phone, department, position,
    last_login_time, created_at, updated_at, deleted
FROM kb_user.kb_user;

-- =====================================================
-- View: kb_comment (source: kb_document.tb_comment)
-- =====================================================
CREATE VIEW `kb_comment` AS
SELECT
    id, document_id, content, user_id, user_name,
    user_avatar, parent_id, reply_to_id, reply_to_name,
    like_count, reply_count, status, created_at,
    updated_at, create_by, update_by, deleted
FROM kb_document.tb_comment;

-- =====================================================
-- View: kb_operation_log (source: kb_foundation.kb_operation_log)
-- =====================================================
CREATE VIEW `kb_operation_log` AS
SELECT
    id, module, operation_type, operation_desc,
    request_method, request_url, request_params,
    response_result, user_id, username, ip_address,
    location, user_agent, execute_time, status,
    error_msg, created_at, updated_at, create_by,
    update_by, deleted
FROM kb_foundation.kb_operation_log;

-- =====================================================
-- View: kb_category (source: kb_document.kb_category)
-- =====================================================
DROP VIEW IF EXISTS `kb_category`;
CREATE VIEW `kb_category` AS
SELECT
    id, category_name, category_code, parent_id, category_icon,
    description, sort, document_count, status,
    created_at, updated_at, create_by, update_by, deleted
FROM kb_document.kb_category;

SELECT 'kb_statistics cross-database views created!' AS message;
