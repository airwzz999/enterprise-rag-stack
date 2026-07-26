package com.knowledge.base.document.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * File upload response
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File upload response")
public class FileUploadResponse {

    @Schema(description = "File access URL")
    private String url;

    @Schema(description = "Original file name")
    private String fileName;

    @Schema(description = "File size (bytes)")
    private Long fileSize;

    @Schema(description = "File size (human-readable)")
    private String fileSizeReadable;
}
