package com.knowledge.base.document.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Image URL conversion response
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Image URL conversion response")
public class ImageConvertResponse {

    @Schema(description = "Original image URL")
    private String originalUrl;

    @Schema(description = "Converted image URL")
    private String convertedUrl;
}
