package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document version restore DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document version restore parameters")
public class DocumentVersionRestoreDTO {

    /**
     * Version ID
     */
    @Schema(description = "Version ID")
    @NotNull(message = "Version ID must not be null")
    private Long versionId;

    /**
     * Restore reason
     */
    @Schema(description = "Restore reason")
    private String reason;
}
