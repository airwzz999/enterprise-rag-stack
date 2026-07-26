package com.knowledge.base.foundation.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * System configuration query DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used for
 * system configuration query conditions</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "System configuration query parameters")
public class SystemConfigQueryDTO extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Config key
     */
    @Schema(description = "Config key", example = "ai.model.name")
    private String configKey;

    /**
     * Config category: AI/STORAGE/NOTIFICATION/SECURITY, etc.
     */
    @Schema(description = "Config category", example = "AI")
    private String category;

    /**
     * Config type: string/number/boolean/json
     */
    @Schema(description = "Config type", example = "string")
    private String configType;

    /**
     * Is public: 0-private, 1-public
     */
    @Schema(description = "Is public", example = "0")
    private Integer isPublic;

    /**
     * Keyword search (config key or description)
     */
    @Schema(description = "Keyword", example = "AI")
    private String keyword;
}
