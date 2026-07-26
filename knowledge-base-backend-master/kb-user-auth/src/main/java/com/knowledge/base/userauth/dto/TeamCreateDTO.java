package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Team creation DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Team creation request")
public class TeamCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Team name
     */
    @Schema(description = "Team name")
    @NotBlank(message = "Team name must not be blank")
    @Size(max = 100, message = "Team name must not exceed 100 characters")
    private String teamName;

    /**
     * Team code
     */
    @Schema(description = "Team code")
    @NotBlank(message = "Team code must not be blank")
    @Size(max = 50, message = "Team code must not exceed 50 characters")
    private String teamCode;

    /**
     * Team description
     */
    @Schema(description = "Team description")
    @Size(max = 500, message = "Team description must not exceed 500 characters")
    private String description;

    /**
     * Team icon
     */
    @Schema(description = "Team icon")
    @Size(max = 50, message = "Icon must not exceed 50 characters")
    private String icon;

    /**
     * Parent team ID
     */
    @Schema(description = "Parent team ID")
    private Long parentId;

    /**
     * Team leader ID
     */
    @Schema(description = "Team leader ID")
    @NotNull(message = "Team leader must not be null")
    private Long leaderId;
}
