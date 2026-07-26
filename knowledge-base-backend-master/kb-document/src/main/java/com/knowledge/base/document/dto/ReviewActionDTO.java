package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Review action DTO (single review)
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Review action parameters")
public class ReviewActionDTO {

    @NotBlank(message = "Review result must not be blank")
    @Schema(description = "Review result: approved-approved, rejected-rejected", example = "approved")
    private String status;

    @Schema(description = "Review comment")
    private String comment;
}
