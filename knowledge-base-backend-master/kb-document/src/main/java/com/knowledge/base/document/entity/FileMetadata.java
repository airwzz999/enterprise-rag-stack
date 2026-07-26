package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * File metadata entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_file_metadata")
public class FileMetadata extends BaseEntity {

    /**
     * File name
     */
    private String fileName;

    /**
     * Original file name
     */
    private String originalFileName;

    /**
     * File extension
     */
    private String fileExtension;

    /**
     * File size (bytes)
     */
    private Long fileSize;

    /**
     * File type (MIME type)
     */
    private String contentType;

    /**
     * File storage path
     */
    private String storagePath;

    /**
     * File access URL
     */
    private String accessUrl;

    /**
     * File category (image, document, video, audio, other)
     */
    private String fileCategory;

    /**
     * Uploader user ID
     */
    private Long uploaderId;

    /**
     * Uploader user name
     */
    private String uploaderName;

    /**
     * File MD5
     */
    private String fileMd5;

    /**
     * File SHA256
     */
    private String fileSha256;

    /**
     * File width (images)
     */
    private Integer width;

    /**
     * File height (images)
     */
    private Integer height;

    /**
     * Thumbnail URL
     */
    private String thumbnailUrl;

    /**
     * Whether public
     */
    private Boolean isPublic;

    /**
     * Download count
     */
    private Integer downloadCount;

    /**
     * Last access time
     */
    private LocalDateTime lastAccessTime;

    /**
     * File status (uploading, completed, failed)
     */
    private String uploadStatus;

    /**
     * Error message
     */
    private String errorMessage;
}
