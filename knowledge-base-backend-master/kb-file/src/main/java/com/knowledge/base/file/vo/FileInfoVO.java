package com.knowledge.base.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * File info VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File info")
public class FileInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * File ID
     */
    @Schema(description = "File ID")
    private Long id;

    /**
     * Original file name
     */
    @Schema(description = "Original file name")
    private String originalName;

    /**
     * File size (bytes)
     */
    @Schema(description = "File size")
    private Long fileSize;

    /**
     * File size (human-readable)
     */
    @Schema(description = "File size (human-readable)")
    private String fileSizeReadable;

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
     * File URL
     */
    @Schema(description = "File URL")
    private String fileUrl;

    /**
     * Preview URL
     */
    @Schema(description = "Preview URL")
    private String previewUrl;

    /**
     * Uploader ID
     */
    @Schema(description = "Uploader ID")
    private Long uploaderId;

    /**
     * Uploader name
     */
    @Schema(description = "Uploader name")
    private String uploaderName;

    /**
     * Access level
     */
    @Schema(description = "Access level")
    private Integer accessLevel;

    /**
     * Download count
     */
    @Schema(description = "Download count")
    private Integer downloadCount;

    /**
     * Storage type
     */
    @Schema(description = "Storage type")
    private String storageType;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

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
     * HLS playback URL
     */
    @Schema(description = "HLS playback URL")
    private String playUrl;

    /**
     * Thumbnail URL
     */
    @Schema(description = "Thumbnail URL")
    private String thumbnailUrl;
}
