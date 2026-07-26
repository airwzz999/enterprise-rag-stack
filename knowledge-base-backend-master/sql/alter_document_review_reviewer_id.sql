-- ========================================
-- Make reviewer_id nullable in the tb_document_review table
-- Reason: when review is submitted, no reviewer has been assigned yet; the reviewer is filled in when the review action is performed
-- ========================================

USE kb_document;

ALTER TABLE `tb_document_review`
    MODIFY COLUMN `reviewer_id` BIGINT(20) NULL DEFAULT NULL COMMENT 'Reviewer ID (NULL when submitted for review, filled in when reviewed)';
