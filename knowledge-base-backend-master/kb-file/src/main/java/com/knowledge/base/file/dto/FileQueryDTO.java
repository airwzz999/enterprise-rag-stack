package com.knowledge.base.file.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * File query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "File query request")
public class FileQueryDTO extends PageParam {

    private static final long serialVersionUID = 1L;

    /**
     * Original file name (fuzzy search)
     */
    @Schema(description = "File name")
    private String originalName;

    /**
     * File type
     */
    @Schema(description = "File type")
    private String fileType;

    /**
     * Uploader ID
     */
    @Schema(description = "Uploader ID")
    private Long uploaderId;

    /**
     * Access level
     */
    @Schema(description = "Access level")
    private Integer accessLevel;
}
