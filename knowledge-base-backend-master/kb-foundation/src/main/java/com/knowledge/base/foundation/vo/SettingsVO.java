package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * System settings response VO
 *
 * <p>Returns system configuration grouped by business area to the frontend, making it
 * easy for the settings page to display them by tab</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "System settings")
public class SettingsVO implements Serializable {

    @Schema(description = "Basic settings")
    private Map<String, Object> basic;

    @Schema(description = "Security settings")
    private Map<String, Object> security;

    @Schema(description = "Storage settings")
    private Map<String, Object> storage;

    @Schema(description = "Notification settings")
    private Map<String, Object> notification;

    @Schema(description = "AI settings")
    private Map<String, Object> ai;

    @Schema(description = "System status")
    private SystemStatusVO status;
}
