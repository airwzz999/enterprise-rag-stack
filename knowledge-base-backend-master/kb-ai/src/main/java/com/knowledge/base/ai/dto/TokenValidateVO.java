package com.knowledge.base.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Token validation response VO (local DTO for kb-ai)
 *
 * <p>Kept structurally consistent with the TokenValidateVO in kb-user-auth,
 * used as the response type for Feign calls.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private Integer status;

    /** List of role codes, e.g. ["ROLE_USER", "ROLE_REVIEWER"] */
    private List<String> roles;

    /** Whether the token is valid */
    private Boolean valid;
}
