-- =====================================================
-- kb_ai database - seed data
-- =====================================================

SET NAMES utf8mb4;
USE `kb_ai`;

-- Seed AI conversation data
INSERT INTO `kb_ai_conversation` (`id`, `user_id`, `user_name`, `title`, `model_name`, `message_count`) VALUES
(1600000000000000001, 1000000000000000001, 'admin', 'Discussion about Spring Boot', 'qwen-turbo', 2),
(1600000000000000002, 1000000000000000002, 'editor', 'Frontend development questions', 'qwen-turbo', 2),
(1600000000000000003, 1000000000000000004, 'developer', 'Database optimization suggestions', 'qwen-turbo', 2);

-- Seed AI message data
INSERT INTO `kb_ai_message` (`id`, `conversation_id`, `role`, `content`, `tokens`) VALUES
(1700000000000000001, 1600000000000000001, 'user', 'What is the principle behind Spring Boot auto-configuration?', 20),
(1700000000000000002, 1600000000000000001, 'assistant', 'Spring Boot auto-configuration is implemented via conditional annotations (@ConditionalOnClass, @ConditionalOnMissingBean, etc.). It decides whether to load a given configuration based on the jars present on the classpath and the beans already defined...', 150),
(1700000000000000003, 1600000000000000002, 'user', 'What are the new features in React 18?', 18),
(1700000000000000004, 1600000000000000002, 'assistant', 'The main new features in React 18 include: 1. Concurrent rendering 2. Automatic batching 3. Transitions 4. Suspense improvements...', 120),
(1700000000000000005, 1600000000000000003, 'user', 'How can I optimize MySQL query performance?', 15),
(1700000000000000006, 1600000000000000003, 'assistant', 'MySQL query optimization can start from several angles: 1. Index optimization 2. Query statement optimization 3. Table structure optimization 4. Parameter tuning...', 135);

-- Update the message count for each conversation
UPDATE `kb_ai_conversation` SET `message_count` = 2 WHERE `id` = 1600000000000000001;
UPDATE `kb_ai_conversation` SET `message_count` = 2 WHERE `id` = 1600000000000000002;
UPDATE `kb_ai_conversation` SET `message_count` = 2 WHERE `id` = 1600000000000000003;

SELECT 'kb_ai seed data initialization complete!' AS message;
SELECT CONCAT('Conversation count: ', COUNT(*)) AS info FROM `kb_ai_conversation`;
SELECT CONCAT('Message count: ', COUNT(*)) AS info FROM `kb_ai_message`;
