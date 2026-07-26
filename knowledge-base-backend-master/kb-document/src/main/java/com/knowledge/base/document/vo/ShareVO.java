package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Document share response VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document share response information")
public class ShareVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Share ID (unique identifier)
     */
    @Schema(description = "Share ID")
    private String shareId;

    /**
     * Share link
     */
    @Schema(description = "Share link")
    private String shareUrl;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Share title
     */
    @Schema(description = "Share title")
    private String title;

    /**
     * Share type (1-public link, 2-direct message share)
     */
    @Schema(description = "Share type (1-public link, 2-direct message share)")
    private Integer shareType;

    /**
     * Share type description
     */
    @Schema(description = "Share type description")
    private String shareTypeDesc;

    /**
     * Expiration type (1-permanent, 2-time-limited)
     */
    @Schema(description = "Expiration type (1-permanent, 2-time-limited)")
    private Integer expireType;

    /**
     * Expiration time
     */
    @Schema(description = "Expiration time")
    private LocalDateTime expireTime;

    /**
     * Whether expired
     */
    @Schema(description = "Whether expired")
    private Boolean expired;

    /**
     * Whether a password is required
     */
    @Schema(description = "Whether a password is required")
    private Boolean requirePassword;

    /**
     * Sharer name
     */
    @Schema(description = "Sharer name")
    private String sharerName;

    /**
     * Share time
     */
    @Schema(description = "Share time")
    private LocalDateTime shareTime;

    /**
     * Access count
     */
    @Schema(description = "Access count")
    private Integer accessCount;

    /**
     * Share description
     */
    @Schema(description = "Share description")
    private String description;
}
