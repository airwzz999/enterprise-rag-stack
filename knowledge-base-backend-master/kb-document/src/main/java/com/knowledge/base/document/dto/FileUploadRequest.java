package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * File upload request DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File upload request")
public class FileUploadRequest implements Serializable {

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
