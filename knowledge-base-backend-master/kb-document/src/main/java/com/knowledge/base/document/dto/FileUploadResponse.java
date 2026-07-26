package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * File upload response DTO
 *
 * <p>Field names are kept consistent with the file service's FileInfoVO to ensure correct deserialization</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File upload response")
public class FileUploadResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "File ID")
    private Long id;

    @Schema(description = "Original file name")
    private String originalName;

    @Schema(description = "File size (bytes)")
    private Long fileSize;

    @Schema(description = "File size (human-readable)")
    private String fileSizeReadable;

    @Schema(description = "File type")
    private String fileType;

    @Schema(description = "MIME type")
    private String mimeType;

    @Schema(description = "File access URL")
    private String fileUrl;

    @Schema(description = "Preview URL")
    private String previewUrl;

    @Schema(description = "Uploader ID")
    private Long uploaderId;

    @Schema(description = "Access level")
    private Integer accessLevel;

    @Schema(description = "Download count")
    private Integer downloadCount;

    @Schema(description = "Storage type")
    private String storageType;

    @Schema(description = "Converted URL (used for the URL conversion interface)")
    private String convertedUrl;

    @Schema(description = "New URL (used for the URL conversion interface, corresponds to UrlConvertResponse's newUrl field)")
    private String newUrl;
}
