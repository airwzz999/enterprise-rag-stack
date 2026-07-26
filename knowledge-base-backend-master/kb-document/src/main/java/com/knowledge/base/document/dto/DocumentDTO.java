package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Document DTO
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, used to receive document create/update request parameters</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Document information request parameters")
public class DocumentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Document ID
     */
    @Schema(description = "Document ID", example = "1234567890123456789")
    private Long id;

    /**
     * Document title
     */
    @Schema(description = "Document title", required = true, example = "Spring Boot Usage Guide")
    @NotBlank(message = "Document title must not be blank")
    @Size(max = 200, message = "Document title must not exceed 200 characters")
    private String title;

    /**
     * Document summary
     */
    @Schema(description = "Document summary", example = "This document introduces the basic usage of Spring Boot")
    @Size(max = 500, message = "Document summary must not exceed 500 characters")
    private String summary;

    /**
     * Document content
     */
    @Schema(description = "Document content")
    private String content;

    /**
     * Document type (1-article, 2-file)
     */
    @Schema(description = "Document type (1-article, 2-file)", example = "1")
    private Integer documentType;

    /**
     * Category ID
     */
    @Schema(description = "Category ID", example = "1234567890123456789")
    private Long categoryId;

    /**
     * Team space ID
     */
    @Schema(description = "Team space ID", example = "1234567890123456789")
    private Long teamId;

    /**
     * Tags (comma-separated)
     */
    @Schema(description = "Tags (comma-separated)", example = "Spring Boot,Java,Backend")
    @Size(max = 200, message = "Tags must not exceed 200 characters")
    private String tags;

    /**
     * Status (0-draft, 1-published, 2-archived)
     */
    @Schema(description = "Status (0-draft, 1-published, 2-archived)", example = "1")
    private Integer status;

    /**
     * Whether pinned to top
     */
    @Schema(description = "Whether pinned to top (0-no, 1-yes)", example = "0")
    private Integer isTop;

    /**
     * Whether recommended
     */
    @Schema(description = "Whether recommended (0-no, 1-yes)", example = "0")
    private Integer isRecommend;

    /**
     * Cover image URL
     */
    @Schema(description = "Cover image URL", example = "https://example.com/cover.jpg")
    @Size(max = 500, message = "Cover image URL must not exceed 500 characters")
    private String coverImage;

    /**
     * Source (1-original, 2-reposted, 3-translated)
     */
    @Schema(description = "Source (1-original, 2-reposted, 3-translated)", example = "1")
    private Integer source;

    /**
     * Source URL
     */
    @Schema(description = "Source URL", example = "https://example.com/original-article")
    @Size(max = 500, message = "Source URL must not exceed 500 characters")
    private String sourceUrl;

    /**
     * Allow comments
     */
    @Schema(description = "Allow comments (0-no, 1-yes)", example = "1")
    private Integer allowComment;

    /**
     * Whether public (0-private, 1-public)
     */
    @Schema(description = "Whether public (0-private, 1-public)", example = "1")
    private Integer isPublic;

    /**
     * Sort order
     */
    @Schema(description = "Sort order", example = "0")
    private Integer sort;

    /**
     * Remark
     */
    @Schema(description = "Remark", example = "This is a remark")
    @Size(max = 500, message = "Remark must not exceed 500 characters")
    private String remark;

    /**
     * File size (bytes)
     */
    @Schema(description = "File size (bytes)", example = "102400")
    private Long fileSize;

    /**
     * File path (storage URL after file upload)
     */
    @Schema(description = "File path", example = "http://example.com/files/doc.pdf")
    private String filePath;

    /**
     * File extension
     */
    @Schema(description = "File extension", example = "pdf")
    private String fileExtension;
}
