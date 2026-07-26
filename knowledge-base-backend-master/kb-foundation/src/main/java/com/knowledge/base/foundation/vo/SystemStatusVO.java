package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * System status VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "System status")
public class SystemStatusVO implements Serializable {

    @Schema(description = "System version")
    private String version;

    @Schema(description = "Run status: running/stopped/maintenance")
    private String runStatus;

    @Schema(description = "Database connection status: connected/disconnected")
    private String dbStatus;

    @Schema(description = "Last backup time")
    private String lastBackupTime;

    @Schema(description = "Total storage space (bytes)")
    private Long totalStorage;

    @Schema(description = "Used storage space (bytes)")
    private Long usedStorage;

    @Schema(description = "Total document count")
    private Long documentCount;

    @Schema(description = "Total user count")
    private Long userCount;

    @Schema(description = "System start time")
    private String startTime;
}
