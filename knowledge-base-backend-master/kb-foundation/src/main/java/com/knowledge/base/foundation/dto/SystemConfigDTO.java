package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * System configuration DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to receive
 * request parameters for creating/updating system configuration</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "System configuration request parameters")
public class SystemConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Config ID
     */
    @Schema(description = "Config ID", example = "1234567890123456789")
    private Long id;

    /**
     * Config key
     */
    @Schema(description = "Config key", required = true, example = "ai.model.name")
    @NotBlank(message = "Config key must not be empty")
    @Size(max = 100, message = "Config key must not exceed 100 characters")
    private String configKey;

    /**
     * Config value
     */
    @Schema(description = "Config value", required = true, example = "gpt-4")
    @NotBlank(message = "Config value must not be empty")
    @Size(max = 1000, message = "Config value must not exceed 1000 characters")
    private String configValue;

    /**
     * Config type: string/number/boolean/json
     */
    @Schema(description = "Config type", required = true, example = "string")
    @NotBlank(message = "Config type must not be empty")
    @Size(max = 20, message = "Config type must not exceed 20 characters")
    private String configType;

    /**
     * Config category: AI/STORAGE/NOTIFICATION/SECURITY, etc.
     */
    @Schema(description = "Config category", required = true, example = "AI")
    @NotBlank(message = "Config category must not be empty")
    @Size(max = 50, message = "Config category must not exceed 50 characters")
    private String category;

    /**
     * Config description
     */
    @Schema(description = "Config description", example = "AI model name configuration")
    @Size(max = 500, message = "Config description must not exceed 500 characters")
    private String description;

    /**
     * Is public: 0-private, 1-public
     */
    @Schema(description = "Is public", example = "0")
    @NotNull(message = "Is public must not be empty")
    private Integer isPublic;
}
