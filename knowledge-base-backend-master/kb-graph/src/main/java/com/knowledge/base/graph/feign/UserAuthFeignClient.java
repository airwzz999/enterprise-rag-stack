package com.knowledge.base.graph.feign;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.graph.dto.TokenValidateVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * User authentication Feign client
 *
 * <p>Calls kb-user-auth's /auth/validate endpoint via Feign, reusing its JWT parsing,
 * blacklist checks, and role lookup logic instead of duplicating them here.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@FeignClient(
        name = "kb-user-auth",
        url = "${kb-user-auth.url:#{null}}",
        path = "/auth",
        fallbackFactory = UserAuthFeignFallbackFactory.class
)
public interface UserAuthFeignClient {

    /**
     * Validate a JWT token
     *
     * @param authorization the Authorization request header (Bearer &lt;token&gt;)
     * @param token the token parameter (alternative delivery method)
     * @return the token validation result, including valid, userId, roles, etc.
     */
    @PostMapping("/validate")
    Result<TokenValidateVO> validateToken(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "token", required = false) String token);
}
