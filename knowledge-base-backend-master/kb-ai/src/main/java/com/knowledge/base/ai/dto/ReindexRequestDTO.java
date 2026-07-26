package com.knowledge.base.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Reindex request DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Reindex request")
public class ReindexRequestDTO {

    @Schema(description = "List of document IDs")
    private List<Long> documentIds;
}
