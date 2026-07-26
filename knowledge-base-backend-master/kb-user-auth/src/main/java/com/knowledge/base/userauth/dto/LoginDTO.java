package com.knowledge.base.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * User login DTO
 *
 * <p>Designed following the Alibaba Java Development Guidelines; used to receive user login request parameters</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "User login request parameters")
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Username
     */
    @Schema(description = "Username", required = true, example = "admin")
    @NotBlank(message = "Username must not be blank")
    private String username;

    /**
     * Password
     */
    @Schema(description = "Password", required = true, example = "123456")
    @NotBlank(message = "Password must not be blank")
    private String password;

    /**
     * Captcha
     */
    @Schema(description = "Captcha", example = "1234")
    private String captcha;

    /**
     * Captcha key
     */
    @Schema(description = "Captcha key")
    private String captchaKey;
}
