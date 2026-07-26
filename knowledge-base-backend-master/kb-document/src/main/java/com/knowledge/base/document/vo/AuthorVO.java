package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Author information VO
 *
 * <p>Used to return detailed information about a document author</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Author information")
public class AuthorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Author ID
     */
    @Schema(description = "Author ID")
    private Long id;

    /**
     * Username
     */
    @Schema(description = "Username")
    private String username;

    /**
     * Email
     */
    @Schema(description = "Email")
    private String email;

    /**
     * Avatar URL
     */
    @Schema(description = "Avatar URL")
    private String avatar;

    /**
     * Position
     */
    @Schema(description = "Position")
    private String position;
}
