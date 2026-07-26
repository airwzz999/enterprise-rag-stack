package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch review DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Batch review parameters")
public class BatchReviewDTO {

    @NotEmpty(message = "Review task ID list must not be empty")
    @Schema(description = "Review task ID list")
    private List<Long> taskIds;

    @NotBlank(message = "Review result must not be blank")
    @Schema(description = "Review result: approved-approved, rejected-rejected", example = "approved")
    private String status;

    @Schema(description = "Review comment (recommended when rejecting)")
    private String comment;
}
