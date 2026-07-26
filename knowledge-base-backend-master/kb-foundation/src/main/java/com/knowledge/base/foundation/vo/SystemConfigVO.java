package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * System configuration VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "System configuration VO")
public class SystemConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Config ID")
    private Long id;

    @Schema(description = "Config key")
    private String configKey;

    @Schema(description = "Config value")
    private String configValue;

    @Schema(description = "Config type")
    private String configType;

    @Schema(description = "Config category")
    private String category;

    @Schema(description = "Config description")
    private String description;

    @Schema(description = "Is public")
    private Integer isPublic;

    @Schema(description = "Created time")
    private LocalDateTime createdAt;

    @Schema(description = "Updated time")
    private LocalDateTime updatedAt;
}
