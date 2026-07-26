package com.knowledge.base.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Date range DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Date range")
public class DateRangeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Start time
     */
    @Schema(description = "Start time")
    @NotNull(message = "Start time must not be null")
    private LocalDateTime startTime;

    /**
     * End time
     */
    @Schema(description = "End time")
    @NotNull(message = "End time must not be null")
    private LocalDateTime endTime;

    /**
     * Time range type (custom, today, yesterday, week, month, year)
     */
    @Schema(description = "Time range type")
    private String rangeType;

    /**
     * Whether the end time includes the current time
     */
    @Schema(description = "Whether to include the current time")
    private Boolean includeCurrent;
}
