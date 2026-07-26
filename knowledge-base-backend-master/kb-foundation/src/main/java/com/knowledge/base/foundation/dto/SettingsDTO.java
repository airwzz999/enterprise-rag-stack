package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * System settings update request DTO
 *
 * <p>Supports batch updating settings by section. Using a Map structure allows flexible
 * extension without needing to modify the DTO fields every time a new setting is added</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Settings update request")
public class SettingsDTO {

    @NotBlank(message = "Settings section must not be empty")
    @Schema(description = "Settings section: basic/security/storage/notification/ai", requiredMode = Schema.RequiredMode.REQUIRED)
    private String section;

    @Schema(description = "Settings key-value pairs", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> settings;
}
