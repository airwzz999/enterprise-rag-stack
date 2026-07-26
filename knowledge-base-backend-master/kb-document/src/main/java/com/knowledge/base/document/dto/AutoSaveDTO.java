package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Auto-save DTO
 *
 * <p>Key differences from DocumentDTO: title is optional, and there is no status field (the backend
 * forces the draft status). Used for the frontend editor's auto-save scenario, allowing users to save
 * progress before completing the full form.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Auto-save request parameters - allows an empty title, forces draft status")
public class AutoSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Document ID (provided when updating, left blank when creating)
     */
    @Schema(description = "Document ID (provided when updating, left blank when creating)", example = "1234567890123456789")
    private Long id;

    /**
     * Document title (may be empty; the backend auto-fills "Untitled Document")
     */
    @Schema(description = "Document title (may be empty)", example = "Spring Boot Usage Guide")
    @Size(max = 200, message = "Document title must not exceed 200 characters")
    private String title;

    /**
     * Document content
     */
    @Schema(description = "Document content")
    private String content;

    /**
     * Document summary
     */
    @Schema(description = "Document summary")
    @Size(max = 500, message = "Document summary must not exceed 500 characters")
    private String summary;

    /**
     * Category ID
     */
    @Schema(description = "Category ID", example = "1234567890123456789")
    private Long categoryId;

    /**
     * Team space ID
     */
    @Schema(description = "Team space ID", example = "1234567890123456789")
    private Long teamId;

    /**
     * Tags (comma-separated)
     */
    @Schema(description = "Tags (comma-separated)", example = "Spring Boot,Java")
    @Size(max = 200, message = "Tags must not exceed 200 characters")
    private String tags;
}
