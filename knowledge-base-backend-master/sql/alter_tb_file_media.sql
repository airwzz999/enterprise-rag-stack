-- =====================================================
-- kb-file audio/video feature extension — new columns on the tb_file table
-- Adds media metadata, transcode status, HLS path, and thumbnail path columns
-- =====================================================

ALTER TABLE tb_file
    ADD COLUMN duration         INT          COMMENT 'Duration (seconds)',
    ADD COLUMN resolution       VARCHAR(20)  COMMENT 'Resolution, e.g. 1920x1080',
    ADD COLUMN bitrate          INT          COMMENT 'Bitrate (kbps)',
    ADD COLUMN transcode_status VARCHAR(20)  DEFAULT NULL COMMENT 'Transcode status: PENDING/PROCESSING/DONE/FAILED',
    ADD COLUMN hls_path         VARCHAR(500) COMMENT 'HLS playlist directory path (relative to bucket)',
    ADD COLUMN thumbnail_path   VARCHAR(500) COMMENT 'Thumbnail path (relative to bucket)';

-- Add an index on the transcode status column to speed up frontend polling queries
CREATE INDEX idx_transcode_status ON tb_file(transcode_status);
