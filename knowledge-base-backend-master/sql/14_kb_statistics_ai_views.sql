-- =====================================================
-- kb_statistics database - AI service cross-database views
-- Provides the statistics service transparent read-only access to AI conversation data
-- =====================================================

SET NAMES utf8mb4;
USE `kb_statistics`;

-- Drop existing views if present (idempotent re-run)
DROP VIEW IF EXISTS `kb_ai_conversation`;
DROP VIEW IF EXISTS `kb_ai_message`;

-- =====================================================
-- View: kb_ai_conversation (source: kb_ai.conversation)
-- =====================================================
CREATE VIEW `kb_ai_conversation` AS
SELECT
    id, title, user_id, model, system_prompt,
    tokens_used, message_count, status,
    created_at, updated_at, deleted
FROM kb_ai.conversation;

-- =====================================================
-- View: kb_ai_message (source: kb_ai.message)
-- =====================================================
CREATE VIEW `kb_ai_message` AS
SELECT
    id, conversation_id, role, content, tokens,
    created_at, deleted
FROM kb_ai.message;

SELECT 'kb_statistics AI views created!' AS message;
