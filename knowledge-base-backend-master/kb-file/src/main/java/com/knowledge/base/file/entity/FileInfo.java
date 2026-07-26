package com.knowledge.base.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * File info entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode
@TableName("tb_file")
@Schema(description = "File info entity")
public class FileInfo {

    private static final long serialVersionUID = 1L;

    /**
     * File ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "File ID")
    private Long id;

    /**
     * Original file name
     */
    @Schema(description = "Original file name")
    private String originalName;

    /**
     * Stored file name
     */
    @Schema(description = "Stored file name")
    private String storedName;

    /**
     * File path
     */
    @Schema(description = "File path")
    private String filePath;

    /**
     * File size (bytes)
     */
    @Schema(description = "File size")
    private Long fileSize;

    /**
     * File type
     */
    @Schema(description = "File type")
    private String fileType;

    /**
     * MIME type
     */
    @Schema(description = "MIME type")
    private String mimeType;

    /**
     * File hash
     */
    @Schema(description = "File hash")
    private String fileHash;

    /**
     * Storage type
     */
    @Schema(description = "Storage type")
    private String storageType;

    /**
     * Bucket name
     */
    @Schema(description = "Bucket name")
    private String bucketName;

    /**
     * Uploader ID
     */
    @Schema(description = "Uploader ID")
    private Long uploaderId;

    /**
     * Access level: 0-private, 1-team visible, 2-public
     */
    @Schema(description = "Access level")
    private Integer accessLevel;

    /**
     * Download count
     */
    @Schema(description = "Download count")
    private Integer downloadCount;

    /**
     * Status: 0-deleted, 1-normal
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Deletion flag
     */
    @TableLogic
    @Schema(description = "Deletion flag")
    private Integer deleted;

    /**
     * Creation time (database field: created_at)
     */
    @TableField("created_at")
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    /**
     * Update time (database field: updated_at)
     */
    @TableField("updated_at")
    @Schema(description = "Update time")
    private LocalDateTime updatedAt;

    /**
     * Creator ID
     */
    @Schema(description = "Creator ID")
    private Long createBy;

    /**
     * Updater ID
     */
    @Schema(description = "Updater ID")
    private Long updateBy;

    /**
     * Duration (seconds), for audio/video files only
     */
    @Schema(description = "Duration (seconds)")
    private Integer duration;

    /**
     * Resolution, e.g. "1920x1080"
     */
    @Schema(description = "Resolution")
    private String resolution;

    /**
     * Bitrate (kbps)
     */
    @Schema(description = "Bitrate (kbps)")
    private Integer bitrate;

    /**
     * Transcode status: PENDING/PROCESSING/DONE/FAILED
     */
    @Schema(description = "Transcode status")
    private String transcodeStatus;

    /**
     * HLS playlist path (relative to the bucket)
     */
    @Schema(description = "HLS playlist path")
    private String hlsPath;

    /**
     * Thumbnail path (relative to the bucket)
     */
    @Schema(description = "Thumbnail path")
    private String thumbnailPath;
}
