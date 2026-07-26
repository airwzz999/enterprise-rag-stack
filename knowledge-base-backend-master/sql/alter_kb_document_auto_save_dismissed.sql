-- Add auto-save draft confirmation flag column
-- Used to mark whether the user has dismissed recovery of the auto-saved draft
ALTER TABLE kb_document ADD COLUMN auto_save_dismissed tinyint(1) DEFAULT 0 COMMENT 'Auto-save draft dismissed (0-not dismissed, 1-user dismissed recovery)';
