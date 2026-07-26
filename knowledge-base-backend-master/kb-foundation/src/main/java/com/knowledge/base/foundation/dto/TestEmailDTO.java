package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Email test request DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Email test request")
public class TestEmailDTO {

    @NotBlank(message = "Email address must not be empty")
    @Email(message = "Please enter a valid email address")
    @Schema(description = "Test email address", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
}
