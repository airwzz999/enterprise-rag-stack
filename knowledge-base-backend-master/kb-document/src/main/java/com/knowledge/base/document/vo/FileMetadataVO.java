package com.knowledge.base.document.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * File metadata VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataVO implements Serializable {

    /**
     * File ID
     */
    private Long id;

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
     * File size (human-readable)
     */
    private String fileSizeReadable;

    /**
     * File type (MIME type)
     */
    private String contentType;

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
     * Whether public
     */
    private Boolean isPublic;

    /**
     * Download count
     */
    private Integer downloadCount;

    /**
     * Creation time
     */
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    private LocalDateTime updatedAt;

    /**
     * Last access time
     */
    private LocalDateTime lastAccessTime;

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
}
