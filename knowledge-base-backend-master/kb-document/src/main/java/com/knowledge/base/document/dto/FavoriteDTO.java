package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Favorite operation DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Favorite operation request parameters")
public class FavoriteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Document ID
     */
    @Schema(description = "Document ID", required = true)
    @NotNull(message = "Document ID must not be null")
    private Long documentId;

    /**
     * Operation type (add-add favorite, remove-remove favorite)
     */
    @Schema(description = "Operation type (add-add favorite, remove-remove favorite)")
    private String action;
}
