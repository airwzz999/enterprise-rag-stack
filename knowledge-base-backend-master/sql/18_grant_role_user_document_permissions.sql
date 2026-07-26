-- =====================================================
-- Grant ROLE_USER (Knowledge Member) document CRUD permissions
--
-- As a regular member of the knowledge base, ROLE_USER needs:
--   document:list   - View document list and details
--   document:create - Create new documents
--   document:edit   - Edit and manage documents
--   document:delete - Delete documents
--
-- Restrictions retained (out of scope for this grant):
--   document:review       - Review permission (ROLE_REVIEWER only)
--   document:category     - Category management (admin only)
--   document:tag          - Tag management (admin only)
--   document:version      - Version management (admin only)
-- =====================================================

SET NAMES utf8mb4;
USE `kb_user`;

-- ROLE_USER role ID
SET @role_user_id = 2000000000000000005;

-- Only insert if the permission has not already been assigned (idempotent)
-- Assign new IDs starting from the current max ID + 1 to avoid conflicts
INSERT INTO `kb_role_permission` (`id`, `role_id`, `permission_id`)
SELECT COALESCE((SELECT MAX(`id`) FROM `kb_role_permission` rp2), 5000000000000000000)
       + (@row_num := @row_num + 1),
       @role_user_id,
       p.`id`
FROM `kb_permission` p, (SELECT @row_num := 0) r
WHERE p.`permission_code` IN (
    'document:list',
    'document:create',
    'document:edit',
    'document:delete'
)
AND NOT EXISTS (
    SELECT 1 FROM `kb_role_permission` rp
    WHERE rp.`role_id` = @role_user_id
      AND rp.`permission_id` = p.`id`
);

-- Output result
SELECT 'ROLE_USER document permission grant complete!' AS message;
SELECT p.`permission_code`, p.`permission_name`
FROM `kb_role_permission` rp
INNER JOIN `kb_permission` p ON rp.`permission_id` = p.`id`
WHERE rp.`role_id` = @role_user_id
  AND p.`permission_code` IN ('document:list', 'document:create', 'document:edit', 'document:delete')
ORDER BY p.`permission_code`;
