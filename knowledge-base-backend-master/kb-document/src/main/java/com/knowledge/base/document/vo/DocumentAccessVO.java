package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Document access record response VO
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, used to return document access record information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Document access record response")
public class DocumentAccessVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Access record ID
     */
    @Schema(description = "Access record ID")
    private Long id;

    /**
     * User ID
     */
    @Schema(description = "User ID")
    private Long userId;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Document title
     */
    @Schema(description = "Document title")
    private String documentTitle;

    /**
     * Document summary
     */
    @Schema(description = "Document summary")
    private String summary;

    /**
     * Category name
     */
    @Schema(description = "Category name")
    private String categoryName;

    /**
     * Author name
     */
    @Schema(description = "Author name")
    private String authorName;

    /**
     * Access time
     */
    @Schema(description = "Access time")
    private LocalDateTime accessTime;

    /**
     * Document status
     */
    @Schema(description = "Document status")
    private Integer status;
}
