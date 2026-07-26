package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Document share request DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document share request parameters")
public class ShareDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Document ID
     */
    @Schema(description = "Document ID", required = true)
    @NotNull(message = "Document ID must not be null")
    private Long documentId;

    /**
     * Share type (1-public link, 2-direct message share)
     */
    @Schema(description = "Share type (1-public link, 2-direct message share)", example = "1")
    @Builder.Default
    private Integer shareType = 1;

    /**
     * Expiration type (1-permanent, 2-time-limited)
     */
    @Schema(description = "Expiration type (1-permanent, 2-time-limited)", example = "1")
    @Builder.Default
    private Integer expireType = 1;

    /**
     * Expiration time (required for time-limited shares)
     */
    @Schema(description = "Expiration time")
    private String expireTime;

    /**
     * Access count limit (0-unlimited)
     */
    @Schema(description = "Access count limit (0-unlimited)", example = "0")
    @Builder.Default
    private Integer accessLimit = 0;

    /**
     * Whether a password is required (0-no, 1-yes)
     */
    @Schema(description = "Whether a password is required (0-no, 1-yes)", example = "0")
    @Builder.Default
    private Integer requirePassword = 0;

    /**
     * Access password
     */
    @Schema(description = "Access password")
    private String password;

    /**
     * Share description
     */
    @Schema(description = "Share description")
    private String description;
}
