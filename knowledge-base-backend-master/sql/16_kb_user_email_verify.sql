-- =====================================================
-- kb_user database - email verification fields upgrade
-- =====================================================

SET NAMES utf8mb4;
USE `kb_user`;

-- Email verification related fields
ALTER TABLE `kb_user`
    ADD COLUMN `email_verified` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether the email has been verified: 0-not verified, 1-verified' AFTER `email`,
    ADD COLUMN `activation_token` VARCHAR(255) DEFAULT NULL COMMENT 'Account activation token' AFTER `email_verified`,
    ADD COLUMN `activation_token_expiry` DATETIME DEFAULT NULL COMMENT 'Activation token expiry time' AFTER `activation_token`;

SELECT 'kb_user email verification fields upgrade complete!' AS message;
