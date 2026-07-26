-- Optimize the tag type column, changing it from VARCHAR to TINYINT
-- Please make sure to back up data before running this

-- 1. Add a new integer-typed column
ALTER TABLE tb_tag ADD COLUMN tag_type_new TINYINT NOT NULL DEFAULT 1 COMMENT 'Tag type: 0-SYSTEM, 1-USER' AFTER category_id;

-- 2. Migrate data: SYSTEM string becomes 0, USER string becomes 1
UPDATE tb_tag SET tag_type_new = CASE
    WHEN tag_type = 'SYSTEM' THEN 0
    WHEN tag_type = 'USER' THEN 1
    ELSE 1
END;

-- 3. Drop the old column
ALTER TABLE tb_tag DROP COLUMN tag_type;

-- 4. Rename the new column
ALTER TABLE tb_tag CHANGE COLUMN tag_type_new tag_type TINYINT NOT NULL DEFAULT 1 COMMENT 'Tag type: 0-SYSTEM, 1-user tag';

-- 5. Update index (if needed)
-- ALTER TABLE tb_tag DROP INDEX idx_tag_type;
-- CREATE INDEX idx_tag_type ON tb_tag(tag_type);
