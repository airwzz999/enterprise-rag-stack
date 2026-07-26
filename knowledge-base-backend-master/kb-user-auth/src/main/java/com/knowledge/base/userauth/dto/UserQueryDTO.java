package com.knowledge.base.userauth.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "User query request")
public class UserQueryDTO extends PageParam {

    private static final long serialVersionUID = 1L;

    /**
     * Username (fuzzy match)
     */
    @Schema(description = "Username")
    private String username;

    /**
     * Real name (fuzzy match)
     */
    @Schema(description = "Real name")
    private String realName;

    /**
     * Email
     */
    @Schema(description = "Email")
    private String email;

    /**
     * Department
     */
    @Schema(description = "Department")
    private String department;

    /**
     * User type
     */
    @Schema(description = "User type")
    private Integer userType;

    /**
     * Status
     */
    @Schema(description = "Status")
    private Integer status;
}
