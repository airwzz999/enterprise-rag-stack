package com.knowledge.base.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI model information VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI model information")
public class ModelVO {

    /**
     * Model key
     */
    @Schema(description = "Model key")
    private String key;

    /**
     * Model display name
     */
    @Schema(description = "Model display name")
    private String displayName;

    /**
     * Model description
     */
    @Schema(description = "Model description")
    private String description;

    /**
     * Whether this is the default model
     */
    @Schema(description = "Whether this is the default model")
    private Boolean isDefault;
}
