package com.knowledge.base.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * File upload DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "File upload request")
public class FileUploadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * Access level: 0-private, 1-team visible, 2-public
     */
    @Schema(description = "Access level")
    private Integer accessLevel;

    /**
     * Team ID
     */
    @Schema(description = "Team ID")
    private Long teamId;
}
