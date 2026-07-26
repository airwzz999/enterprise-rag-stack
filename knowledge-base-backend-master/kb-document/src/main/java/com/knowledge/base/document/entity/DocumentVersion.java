package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Document version entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("tb_document_version")
@Schema(description = "Document version entity")
public class DocumentVersion {

    /**
     * Version ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Version ID")
    private Long id;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Version number
     */
    @Schema(description = "Version number")
    private Integer version;

    /**
     * Document title
     */
    @Schema(description = "Document title")
    private String title;

    /**
     * Document content
     */
    @Schema(description = "Document content")
    private String content;

    /**
     * Document summary
     */
    @Schema(description = "Document summary")
    private String summary;

    /**
     * Version change description
     */
    @Schema(description = "Version change description")
    private String changeDescription;

    /**
     * Change size (bytes)
     */
    @Schema(description = "Change size")
    private Long changeSize;

    /**
     * Operator ID
     */
    @Schema(description = "Operator ID")
    private Long operatorId;

    /**
     * Operator name
     */
    @Schema(description = "Operator name")
    private String operatorName;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
